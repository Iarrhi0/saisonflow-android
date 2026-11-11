package com.saisonflow.app
import org.junit.Assert.*
import org.junit.Test
class Mp4ProbeTest {
    @Test fun acceptsBmffHeader() = assertTrue(Mp4Probe.matches(byteArrayOf(0,0,0,24) + "ftyp".toByteArray()))
    @Test fun rejectsHtmlAndTruncatedResponses() {
        assertFalse(Mp4Probe.matches("<html>ok".toByteArray()))
        assertFalse(Mp4Probe.matches(byteArrayOf(0)))
    }
}
