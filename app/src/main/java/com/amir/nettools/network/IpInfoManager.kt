package com.amir.nettools.network

import com.amir.nettools.domain.model.IpAnalysis
import com.amir.nettools.util.Errs
import com.amir.nettools.util.IpClassifier
import com.amir.nettools.util.IpScope
import com.amir.nettools.util.ReverseDns
import com.amir.nettools.util.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Analyses an IP literal locally; only the optional reverse (PTR) lookup touches the network. */
class IpInfoManager {
    suspend fun analyze(input: String): IpAnalysis {
        val ip = input.trim()
        val analysis = when {
            Validators.isValidIpv4(ip) -> IpAnalysis(
                address = ip,
                version = 4,
                scope = IpClassifier.scopeOfIpv4(ip) ?: IpScope.PUBLIC,
                normalized = ip,
                expanded = null,
                integerValue = IpClassifier.ipv4ToLong(ip),
                reversePointer = IpClassifier.reversePointer(ip),
                reverseDns = null
            )
            Validators.isValidIpv6(ip) -> {
                val bytes = IpClassifier.parseIpv6(ip) ?: throw Errs.invalidIp()
                IpAnalysis(
                    address = ip,
                    version = 6,
                    scope = IpClassifier.scopeOfIpv6(bytes),
                    normalized = IpClassifier.ipv6Compress(bytes),
                    expanded = IpClassifier.ipv6Expanded(bytes),
                    integerValue = null,
                    reversePointer = IpClassifier.reversePointer(ip),
                    reverseDns = null
                )
            }
            else -> throw Errs.invalidIp()
        }
        val rdns = withContext(Dispatchers.IO) { ReverseDns.lookup(ip, 2500) }
        return analysis.copy(reverseDns = rdns)
    }
}
