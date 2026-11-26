package com.amir.nettools.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.amir.nettools.domain.model.ConnType
import com.amir.nettools.domain.model.ConnectivityResult
import com.amir.nettools.domain.model.LocalIpInfo
import com.amir.nettools.domain.model.NetStatus
import com.amir.nettools.domain.model.NetworkInfoData
import com.amir.nettools.domain.model.WifiData
import com.amir.nettools.util.IpClassifier
import com.amir.nettools.util.IpScope
import com.amir.nettools.util.Resolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetworkInfoManager(private val context: Context) {
    private val cm: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wm: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    companion object {
        private val REACH_TARGETS = listOf("1.1.1.1" to 443, "8.8.8.8" to 443, "9.9.9.9" to 443)
    }

    fun hasActiveNetwork(): Boolean = try {
        cm.activeNetwork != null
    } catch (e: Exception) {
        false
    }

    private fun caps(): NetworkCapabilities? = try {
        cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    } catch (e: Exception) {
        null
    }

    private fun linkProps(): LinkProperties? = try {
        cm.activeNetwork?.let { cm.getLinkProperties(it) }
    } catch (e: Exception) {
        null
    }

    fun connType(c: NetworkCapabilities? = caps()): ConnType = when {
        c == null -> ConnType.NONE
        c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnType.WIFI
        c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnType.MOBILE
        c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnType.ETHERNET
        c.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> ConnType.BLUETOOTH
        c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnType.VPN
        else -> ConnType.OTHER
    }

    fun status(): NetStatus {
        val c = caps()
        return NetStatus(connType(c), c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
    }

    fun statusFlow(): Flow<NetStatus> = callbackFlow {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(status())
            }

            override fun onLost(network: Network) {
                trySend(NetStatus(ConnType.NONE, false))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(status())
            }
        }
        trySend(status())
        try {
            cm.registerDefaultNetworkCallback(cb)
        } catch (_: Exception) {
        }
        awaitClose {
            try {
                cm.unregisterNetworkCallback(cb)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged()

    fun dnsServers(): List<InetAddress> = linkProps()?.dnsServers ?: emptyList()

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun localIps(): LocalIpInfo {
        val lp = linkProps()
        val v4 = mutableListOf<String>()
        val v6 = mutableListOf<String>()
        lp?.linkAddresses?.forEach { la ->
            val a = la.address
            val s = Resolver.ip(a)
            if (a is Inet4Address) v4 += s else if (a is Inet6Address) v6 += s
        }
        val priv = v4.firstOrNull { IpClassifier.scopeOfIpv4(it) == IpScope.PRIVATE }
        return LocalIpInfo(v4, v6, priv, lp?.interfaceName, connType())
    }

    @Suppress("DEPRECATION")
    fun wifiData(): WifiData {
        val perm = hasLocationPermission()
        if (connType() != ConnType.WIFI) {
            return WifiData(false, perm, null, null, null, null, null, null, null)
        }
        val info = try {
            wm?.connectionInfo
        } catch (e: SecurityException) {
            null
        }
        val ssid = info?.ssid?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" && it != "0x" }
        val bssid = info?.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
        val rssi = info?.rssi?.takeIf { it in -126..-1 }
        val speed = info?.linkSpeed?.takeIf { it > 0 }
        val freq = info?.frequency?.takeIf { it > 0 }
        val band = freq?.let {
            when (it) {
                in 2400..2500 -> "2.4 GHz"
                in 4900..5900 -> "5 GHz"
                in 5925..7125 -> "6 GHz"
                else -> null
            }
        }
        val ip = localIps().ipv4.firstOrNull()
        return WifiData(true, perm, ssid, bssid, rssi, speed, freq, band, ip)
    }

    fun snapshot(): NetworkInfoData {
        val c = caps()
        val lp = linkProps()
        val type = connType(c)

        val gateways = lp?.routes?.filter { it.isDefaultRoute }
            ?.mapNotNull { it.gateway }
            ?.filter { !it.isAnyLocalAddress }
            .orEmpty()
        val gateway = (gateways.firstOrNull { it is Inet4Address } ?: gateways.firstOrNull())?.let { Resolver.ip(it) }

        val wifi = if (type == ConnType.WIFI) wifiData() else null
        val linkSpeed = wifi?.linkSpeedMbps?.let { "$it Mbps" } ?: c?.let {
            val down = it.linkDownstreamBandwidthKbps
            val up = it.linkUpstreamBandwidthKbps
            if (down > 0 || up > 0) "~${down / 1000} Mbps ↓ / ~${up / 1000} Mbps ↑ (system estimate)" else null
        }

        val capNames = mutableListOf<String>()
        if (c != null) {
            listOf(
                "INTERNET" to NetworkCapabilities.NET_CAPABILITY_INTERNET,
                "VALIDATED" to NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                "NOT_METERED" to NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
                "NOT_ROAMING" to NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
                "NOT_VPN" to NetworkCapabilities.NET_CAPABILITY_NOT_VPN,
                "NOT_RESTRICTED" to NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED,
                "TRUSTED" to NetworkCapabilities.NET_CAPABILITY_TRUSTED,
                "CAPTIVE_PORTAL" to NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL
            ).forEach { (n, k) -> if (c.hasCapability(k)) capNames += n }
        }

        val localIps = lp?.linkAddresses?.map { Resolver.ip(it.address) }.orEmpty()
        val privateDns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && lp != null && lp.isPrivateDnsActive) {
            lp.privateDnsServerName ?: "Automatic"
        } else null

        return NetworkInfoData(
            connType = type,
            hasInternet = c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            validated = c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            ssid = wifi?.ssid,
            linkSpeed = linkSpeed,
            capabilities = capNames,
            localIps = localIps,
            gateway = gateway,
            dnsServers = lp?.dnsServers?.map { Resolver.ip(it) }.orEmpty(),
            privateDns = privateDns,
            iface = lp?.interfaceName
        )
    }

    suspend fun checkConnectivity(): ConnectivityResult = withContext(Dispatchers.IO) {
        val c = caps()
        val available = c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val validated = c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        var reachMs: Long? = null
        var target: String? = null
        if (available) {
            for ((h, p) in REACH_TARGETS) {
                val ms = tcpConnectMs(h, p, 3000)
                if (ms != null) {
                    reachMs = ms
                    target = "$h:$p"
                    break
                }
            }
        }
        ConnectivityResult(available, validated, connType(c), reachMs != null, reachMs, target)
    }

    private fun tcpConnectMs(host: String, port: Int, timeoutMs: Int): Long? {
        val s = Socket()
        return try {
            val t = System.nanoTime()
            s.connect(InetSocketAddress(host, port), timeoutMs)
            (System.nanoTime() - t) / 1_000_000
        } catch (e: IOException) {
            null
        } finally {
            try {
                s.close()
            } catch (_: IOException) {
            }
        }
    }
}
