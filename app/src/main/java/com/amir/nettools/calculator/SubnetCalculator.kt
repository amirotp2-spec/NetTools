package com.amir.nettools.calculator

import com.amir.nettools.domain.model.ResultRows
import com.amir.nettools.util.IpClassifier
import com.amir.nettools.util.tr

data class SubnetResult(
    val address: String,
    val cidr: Int,
    val networkAddress: String,
    val broadcastAddress: String,
    val subnetMask: String,
    val wildcardMask: String,
    val firstUsable: String,
    val lastUsable: String,
    val usableHosts: Long,
    val totalAddresses: Long
) : ResultRows {
    override fun rows(fa: Boolean) = listOf(
        tr(fa, "آدرس ورودی", "Input address") to address,
        "CIDR" to "/$cidr",
        tr(fa, "آدرس شبکه", "Network address") to networkAddress,
        tr(fa, "آدرس Broadcast", "Broadcast address") to broadcastAddress,
        tr(fa, "Subnet mask", "Subnet mask") to subnetMask,
        tr(fa, "Wildcard mask", "Wildcard mask") to wildcardMask,
        tr(fa, "اولین آدرس قابل استفاده", "First usable address") to firstUsable,
        tr(fa, "آخرین آدرس قابل استفاده", "Last usable address") to lastUsable,
        tr(fa, "تعداد میزبان قابل استفاده", "Usable hosts") to usableHosts.toString(),
        tr(fa, "کل آدرس‌ها", "Total addresses") to totalAddresses.toString()
    )
}

object SubnetCalculator {
    private const val ALL = 0xFFFFFFFFL

    /** Parses "a.b.c.d/nn". Returns null if the text is not in that shape. */
    fun parse(input: String): Pair<String, Int>? {
        val parts = input.trim().split("/")
        if (parts.size != 2) return null
        val cidr = parts[1].trim().toIntOrNull() ?: return null
        return parts[0].trim() to cidr
    }

    fun calculate(ip: String, cidr: Int): SubnetResult {
        val addr = IpClassifier.ipv4ToLong(ip) ?: throw IllegalArgumentException("invalid IPv4 address")
        require(cidr in 0..32) { "CIDR must be between 0 and 32" }

        val mask = (ALL shl (32 - cidr)) and ALL
        val wildcard = mask.inv() and ALL
        val network = addr and mask
        val broadcast = network or wildcard
        val total = 1L shl (32 - cidr)

        val first: Long
        val last: Long
        val usable: Long
        when (cidr) {
            32 -> { first = network; last = network; usable = 1 }
            31 -> { first = network; last = broadcast; usable = 2 }
            else -> { first = network + 1; last = broadcast - 1; usable = total - 2 }
        }

        return SubnetResult(
            address = ip,
            cidr = cidr,
            networkAddress = IpClassifier.longToIpv4(network),
            broadcastAddress = IpClassifier.longToIpv4(broadcast),
            subnetMask = IpClassifier.longToIpv4(mask),
            wildcardMask = IpClassifier.longToIpv4(wildcard),
            firstUsable = IpClassifier.longToIpv4(first),
            lastUsable = IpClassifier.longToIpv4(last),
            usableHosts = usable,
            totalAddresses = total
        )
    }
}
