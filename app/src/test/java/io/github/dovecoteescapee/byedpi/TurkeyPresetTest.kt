package io.github.dovecoteescapee.byedpi

import io.github.dovecoteescapee.byedpi.core.ByeDpiProxyCmdPreferences
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkeyPresetTest {
    private val general =
        "--split 1 --disorder 3+s --mod-http=h,d --auto=torst --tlsrec 1+s"

    @Test
    fun general_preset_parses_with_auto_and_tlsrec() {
        val a = ByeDpiProxyCmdPreferences(general).args.toList()
        assertTrue(a.contains("--split"))
        assertTrue(a.contains("--auto=torst"))
        assertTrue(a.contains("--tlsrec"))
        assertTrue(a.containsAll(listOf("--ip", "--port")))
    }

    @Test
    fun ttl_preset_carries_low_ttl() {
        val a = ByeDpiProxyCmdPreferences("--fake 1+s --ttl 4 --auto=torst").args.toList()
        assertTrue(a.contains("--ttl"))
        assertTrue(a.contains("4"))
    }
}
