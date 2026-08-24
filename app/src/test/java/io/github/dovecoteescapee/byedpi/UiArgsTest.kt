package io.github.dovecoteescapee.byedpi

import io.github.dovecoteescapee.byedpi.core.ByeDpiProxyCmdPreferences
import io.github.dovecoteescapee.byedpi.core.ByeDpiProxyUIPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiArgsTest {
    @Test
    fun cmd_injects_ip_and_port_when_absent() {
        val p = ByeDpiProxyCmdPreferences("--split 1 --disorder 3+s --auto=torst")
        val a = p.args.toList()
        assertEquals("ciadpi", a.first())
        assertTrue(a.containsAll(listOf("--ip", "127.0.0.1", "--port", "1080")))
        assertTrue(a.contains("--split"))
    }

    @Test
    fun cmd_does_not_duplicate_explicit_port() {
        val p = ByeDpiProxyCmdPreferences("-p 1081 --split 1")
        assertEquals(1, p.args.count { it == "-p" || it == "--port" })
    }

    @Test
    fun ui_defaults_emit_listen_and_desync() {
        val a = ByeDpiProxyUIPreferences().args.toList()
        assertEquals("ciadpi", a.first())
        assertTrue(a.any { it.startsWith("-i") })
        assertTrue(a.any { it.startsWith("-p") })
    }
}
