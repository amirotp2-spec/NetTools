package com.amir.nettools.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amir.nettools.NetToolsApp
import com.amir.nettools.calculator.SubnetCalculator
import com.amir.nettools.calculator.SubnetResult
import com.amir.nettools.domain.model.*
import com.amir.nettools.util.ErrorMapper
import com.amir.nettools.util.Errs
import com.amir.nettools.util.Fmt2
import com.amir.nettools.util.Validators
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Common Idle/Loading/Success/Error handling + history persistence for every tool. */
abstract class ToolViewModel<T : ResultRows>(
    app: Application,
    private val tool: ToolType
) : AndroidViewModel(app) {

    protected val appCtx: NetToolsApp = app as NetToolsApp

    private val _state = MutableStateFlow<UiState<T>>(UiState.Idle)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()
    private var job: Job? = null

    protected fun execute(
        target: String,
        save: Boolean = true,
        showLoading: Boolean = true,
        summary: (T) -> String,
        block: suspend () -> T
    ) {
        job?.cancel()
        if (showLoading) _state.value = UiState.Loading
        job = viewModelScope.launch {
            try {
                val result = block()
                _state.value = UiState.Success(result)
                if (save) {
                    try {
                        val details = result.rows(appCtx.settings.isFa)
                            .joinToString("\n") { "${it.first}: ${it.second}" }
                        appCtx.historyRepository.add(tool, target, summary(result), details)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // History problems must never turn a successful test into an error.
                    }
                }
            } catch (e: CancellationException) {
                if (job === coroutineContext[Job]) _state.value = UiState.Idle
                throw e
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorMapper.map(e, appCtx.networkInfoManager.hasActiveNetwork()))
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}

class PingViewModel(app: Application) : ToolViewModel<PingResult>(app, ToolType.PING) {
    fun start(host: String, countText: String) {
        val count = Validators.parseCount(countText, 4, 20)
        execute(host.trim(), summary = {
            "${it.received}/${it.sent} • avg " + (it.avgMs?.let { v -> String.format(java.util.Locale.US, "%.1f ms", v) } ?: "-")
        }) {
            if (count == null) throw Errs.invalidCount(20)
            appCtx.pingManager.ping(host, count)
        }
    }
}

class DnsViewModel(app: Application) : ToolViewModel<DnsResult>(app, ToolType.DNS) {
    fun start(host: String, type: DnsType?) {
        val types = if (type == null) DnsType.entries.toList() else listOf(type)
        execute("${host.trim()} (${type?.name ?: "ALL"})", summary = { "${it.records.size} records" }) {
            appCtx.dnsManager.lookup(host, types)
        }
    }
}

class IpViewModel(app: Application) : ToolViewModel<IpAnalysis>(app, ToolType.IP_INFO) {
    private val _local = MutableStateFlow(appCtx.networkInfoManager.localIps())
    val local: StateFlow<LocalIpInfo> = _local.asStateFlow()

    fun refreshLocal() {
        _local.value = appCtx.networkInfoManager.localIps()
    }

    fun analyze(ip: String) {
        execute(ip.trim(), summary = { "IPv${it.version} • ${it.scope.en}" }) {
            appCtx.ipInfoManager.analyze(ip)
        }
    }
}

class HttpViewModel(app: Application) : ToolViewModel<HttpTestResult>(app, ToolType.HTTP) {
    fun start(url: String) {
        execute(url.trim(), summary = { "HTTP ${it.statusCode} • ${it.durationMs} ms" }) {
            appCtx.httpTester.test(url)
        }
    }
}

class ConnectivityViewModel(app: Application) : ToolViewModel<ConnectivityResult>(app, ToolType.CONNECTIVITY) {
    fun check(save: Boolean) {
        execute(
            "connectivity", save = save,
            summary = { if (it.connected) "Connected (${it.type.en})" else "No Internet" }
        ) { appCtx.networkInfoManager.checkConnectivity() }
    }
}

class NetworkInfoViewModel(app: Application) : ToolViewModel<NetworkInfoData>(app, ToolType.NETWORK_INFO) {
    fun refresh(save: Boolean) {
        execute(
            "network", save = save, showLoading = false,
            summary = { "${it.connType.en} • ${it.localIps.firstOrNull() ?: "-"}" }
        ) { appCtx.networkInfoManager.snapshot() }
    }
}

class WifiViewModel(app: Application) : ToolViewModel<WifiData>(app, ToolType.WIFI) {
    fun refresh(save: Boolean) {
        execute(
            "wifi", save = save, showLoading = false,
            summary = { if (it.connected) "Wi-Fi ${it.ssid ?: "(SSID unavailable)"}" else "Not on Wi-Fi" }
        ) { appCtx.networkInfoManager.wifiData() }
    }
}

class PortViewModel(app: Application) : ToolViewModel<PortResult>(app, ToolType.PORT) {
    fun start(host: String, portText: String) {
        val port = Validators.parsePort(portText)
        execute("${host.trim()}:${portText.trim()}", summary = { "${it.status.en} • ${it.timeMs} ms" }) {
            if (port == null) throw Errs.invalidPort()
            appCtx.portChecker.check(host, port)
        }
    }
}

class TraceViewModel(app: Application) : ToolViewModel<TraceResult>(app, ToolType.TRACEROUTE) {
    private val _hops = MutableStateFlow<List<TraceHop>>(emptyList())
    val liveHops: StateFlow<List<TraceHop>> = _hops.asStateFlow()

    fun start(host: String) {
        _hops.value = emptyList()
        execute(host.trim(), summary = { "${it.hops.size} hops • " + (if (it.reached) "reached" else "not reached") }) {
            appCtx.traceRouteManager.trace(host) { hop -> _hops.update { l -> l + hop } }
        }
    }
}

class SpeedViewModel(app: Application) : ToolViewModel<SpeedResult>(app, ToolType.SPEED) {
    private val _progress = MutableStateFlow<SpeedProgress?>(null)
    val progress: StateFlow<SpeedProgress?> = _progress.asStateFlow()

    fun start(server: String) {
        _progress.value = null
        execute(server.trim().ifEmpty { "speed.cloudflare.com" }, summary = {
            "↓ " + Fmt2.mbps(it.downloadMbps) + " ↑ " + Fmt2.mbps(it.uploadMbps)
        }) {
            appCtx.speedTestManager.run(server) { p -> _progress.value = p }
        }
    }
}

class PublicIpViewModel(app: Application) : ToolViewModel<PublicIpResult>(app, ToolType.PUBLIC_IP) {
    fun refresh(save: Boolean) {
        execute(
            "public-ip", save = save,
            summary = { (it.ipv4 ?: "-") + (it.ipv6?.let { v6 -> " / $v6" } ?: "") }
        ) { appCtx.publicIpManager.fetch() }
    }
}

class SubnetViewModel(app: Application) : ToolViewModel<SubnetResult>(app, ToolType.SUBNET) {
    fun calculate(ip: String, cidrText: String) {
        val address = ip.trim()
        val cidr = cidrText.trim().removePrefix("/").toIntOrNull()
        execute("$address/${cidrText.trim().removePrefix("/")}", summary = {
            "${it.networkAddress}/${it.cidr} • ${it.usableHosts} hosts"
        }) {
            if (!Validators.isValidIpv4(address)) throw Errs.invalidIp()
            if (cidr == null || cidr !in 0..32) throw Errs.invalidCidr()
            SubnetCalculator.calculate(address, cidr)
        }
    }
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val appCtx = app as NetToolsApp

    val netStatus: StateFlow<NetStatus> = appCtx.networkInfoManager.statusFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetStatus(ConnType.NONE, false))

    private val _publicIp = MutableStateFlow<UiState<PublicIpResult>>(UiState.Idle)
    val publicIp: StateFlow<UiState<PublicIpResult>> = _publicIp.asStateFlow()

    val recent: StateFlow<List<HistoryItem>> = appCtx.historyRepository.observe()
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshPublicIp()
    }

    fun refreshPublicIp() {
        viewModelScope.launch {
            _publicIp.value = UiState.Loading
            _publicIp.value = try {
                UiState.Success(appCtx.publicIpManager.fetch())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(ErrorMapper.map(e, appCtx.networkInfoManager.hasActiveNetwork()))
            }
        }
    }
}

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as NetToolsApp).historyRepository

    val items: StateFlow<List<HistoryItem>> = repo.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clear() {
        viewModelScope.launch { repo.clear() }
    }
}
