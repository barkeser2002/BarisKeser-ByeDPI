package com.bariskeser.byedpi.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class StrategyCandidate(val name: String, val args: String)

enum class StrategyOutcome { Success, Failed, Timeout }

data class StrategyResult(
    val outcome: StrategyOutcome,
    val latencyMs: Long?,
)

/**
 * Runs each candidate command line against a single host, one at a time.
 *
 * For every candidate a fresh [ByeDpiProxy] is started on 127.0.0.1:[PROXY_PORT] in
 * command-line mode, the test waits [SETTLE_DELAY_MS], then an HTTPS request is made
 * through that SOCKS proxy. Any HTTP response counts as success. The proxy is always
 * stopped afterwards, mirroring ByeDpiVpnService.stopProxy (graceful stop, join with a
 * timeout, then force close).
 */
class StrategyTester(private val host: String) {
    companion object {
        const val PROXY_IP = "127.0.0.1"
        const val PROXY_PORT = 10800
        const val CONNECT_TIMEOUT_MS = 5000
        const val READ_TIMEOUT_MS = 5000
        const val SETTLE_DELAY_MS = 400L
        const val STOP_JOIN_TIMEOUT_MS = 3000L
    }

    /**
     * Tests [candidates] in order. [onStart] is called before a candidate is tested and
     * [onResult] after it. Both run in the caller's context; blocking work is moved to
     * Dispatchers.IO internally. Cancelling the caller stops the current proxy.
     */
    suspend fun run(
        candidates: List<StrategyCandidate>,
        onStart: suspend (index: Int) -> Unit,
        onResult: suspend (index: Int, result: StrategyResult) -> Unit,
    ) {
        candidates.forEachIndexed { index, candidate ->
            onStart(index)
            val result = testCandidate(candidate.args)
            onResult(index, result)
        }
    }

    private suspend fun testCandidate(args: String): StrategyResult = coroutineScope {
        val proxy = ByeDpiProxy()
        val preferences = ByeDpiProxyCmdPreferences(
            cmd = args,
            ip = PROXY_IP,
            port = PROXY_PORT.toString(),
        )
        // startProxy blocks until the proxy is stopped, so it gets its own job.
        val proxyJob = launch(Dispatchers.IO) { proxy.startProxy(preferences) }
        try {
            delay(SETTLE_DELAY_MS)
            withContext(Dispatchers.IO) { probe() }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                stopProxy(proxy, proxyJob)
            }
        }
    }

    private fun probe(): StrategyResult {
        val socks = Proxy(Proxy.Type.SOCKS, InetSocketAddress(PROXY_IP, PROXY_PORT))
        var connection: HttpsURLConnection? = null
        return try {
            val conn = URL("https://$host/").openConnection(socks) as HttpsURLConnection
            connection = conn
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.useCaches = false
            conn.instanceFollowRedirects = false

            val start = System.nanoTime()
            val code = conn.responseCode
            val elapsedMs = (System.nanoTime() - start) / 1_000_000L

            if (code < 0) {
                StrategyResult(StrategyOutcome.Failed, null)
            } else {
                StrategyResult(StrategyOutcome.Success, elapsedMs)
            }
        } catch (e: SocketTimeoutException) {
            StrategyResult(StrategyOutcome.Timeout, null)
        } catch (e: IOException) {
            StrategyResult(StrategyOutcome.Failed, null)
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun stopProxy(proxy: ByeDpiProxy, proxyJob: Job) {
        proxy.stopProxy()
        val joined = withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS) { proxyJob.join() }
        if (joined == null) {
            proxy.forceClose()
            proxyJob.join()
        }
    }
}
