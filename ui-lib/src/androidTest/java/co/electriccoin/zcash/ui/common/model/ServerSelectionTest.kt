package co.electriccoin.zcash.ui.common.model

import androidx.test.filters.SmallTest
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerSelectionTest {
    @Test
    @SmallTest
    fun customPersistedEndpointMigratesToManualMode() {
        val endpoint = LightWalletEndpoint(host = "custom.example.com", port = 9067, isSecure = true)

        val selection =
            ServerSelection.fromPersistedEndpoint(
                endpoint = endpoint,
                knownEndpoints = knownEndpoints
            )

        assertEquals(ConnectionMode.MANUAL, selection.mode)
        assertEquals(endpoint, selection.endpoint)
    }

    @Test
    @SmallTest
    fun knownPersistedEndpointMigratesToAutomaticMode() {
        val selection =
            ServerSelection.fromPersistedEndpoint(
                endpoint = knownEndpoints.first(),
                knownEndpoints = knownEndpoints
            )

        assertEquals(ConnectionMode.AUTOMATIC, selection.mode)
        assertNull(selection.endpoint)
    }

    @Test
    @SmallTest
    fun missingPersistedEndpointMigratesToAutomaticMode() {
        val selection =
            ServerSelection.fromPersistedEndpoint(
                endpoint = null,
                knownEndpoints = knownEndpoints
            )

        assertEquals(ConnectionMode.AUTOMATIC, selection.mode)
        assertNull(selection.endpoint)
    }

    companion object {
        private val knownEndpoints =
            listOf(
                LightWalletEndpoint(host = "zec.rocks", port = 443, isSecure = true)
            )
    }
}
