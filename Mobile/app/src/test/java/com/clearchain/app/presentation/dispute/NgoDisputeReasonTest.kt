package com.clearchain.app.presentation.dispute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NgoDisputeReasonTest {

    // Must match DisputeReasons on the server. A change on one side without the other would make
    // every dispute from that side fail or be stored unreadably.
    @Test
    fun keys_match_the_server_reason_keys() {
        assertEquals(
            listOf("poor_condition", "wrong_items", "quantity_mismatch", "expired", "not_available", "other"),
            NgoDisputeReason.entries.map { it.key }
        )
    }

    @Test
    fun every_key_maps_back_to_its_reason() {
        NgoDisputeReason.entries.forEach { reason ->
            assertEquals(reason, NgoDisputeReason.fromKey(reason.key))
        }
    }

    @Test
    fun text_that_is_not_a_key_maps_to_null() {
        assertNull(NgoDisputeReason.fromKey("Poor food condition"))
        assertNull(NgoDisputeReason.fromKey("no_show"))
    }
}
