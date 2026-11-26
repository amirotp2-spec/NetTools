package com.amir.nettools

import android.app.Application
import androidx.room.Room
import com.amir.nettools.data.local.HistoryDatabase
import com.amir.nettools.data.repository.HistoryRepositoryImpl
import com.amir.nettools.domain.repository.HistoryRepository
import com.amir.nettools.network.DnsManager
import com.amir.nettools.network.HttpTester
import com.amir.nettools.network.IpInfoManager
import com.amir.nettools.network.NetworkInfoManager
import com.amir.nettools.network.PingManager
import com.amir.nettools.network.PortChecker
import com.amir.nettools.network.PublicIpManager
import com.amir.nettools.network.SpeedTestManager
import com.amir.nettools.network.TraceRouteManager
import com.amir.nettools.util.SettingsStore

/** Application class doubling as a tiny manual dependency container. */
class NetToolsApp : Application() {
    val settings: SettingsStore by lazy { SettingsStore(this) }
    private val database: HistoryDatabase by lazy {
        Room.databaseBuilder(this, HistoryDatabase::class.java, "nettools.db").build()
    }
    val historyRepository: HistoryRepository by lazy { HistoryRepositoryImpl(database.historyDao()) }

    val networkInfoManager: NetworkInfoManager by lazy { NetworkInfoManager(this) }
    val pingManager: PingManager by lazy { PingManager() }
    val dnsManager: DnsManager by lazy { DnsManager { networkInfoManager.dnsServers() } }
    val httpTester: HttpTester by lazy { HttpTester() }
    val portChecker: PortChecker by lazy { PortChecker() }
    val traceRouteManager: TraceRouteManager by lazy { TraceRouteManager() }
    val publicIpManager: PublicIpManager by lazy { PublicIpManager() }
    val speedTestManager: SpeedTestManager by lazy { SpeedTestManager() }
    val ipInfoManager: IpInfoManager by lazy { IpInfoManager() }
}
