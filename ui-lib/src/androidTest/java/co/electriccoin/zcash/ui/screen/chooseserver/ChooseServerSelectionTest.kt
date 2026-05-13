package co.electriccoin.zcash.ui.screen.chooseserver

import androidx.test.filters.SmallTest
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.zcash.ui.common.model.ConnectionMode
import co.electriccoin.zcash.ui.common.model.ServerSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChooseServerSelectionTest {
    @Test
    @SmallTest
    fun currentKnownEndpointPinsAsManualWhenNoEndpointWasTapped() {
        val endpoint = knownEndpoints[1]

        val selection =
            endpoint.toCurrentManualServerSelection(
                persistedSelection = ServerSelection.automatic(),
                availableServers = knownEndpoints
            )

        assertEquals(ConnectionMode.MANUAL, selection.mode)
        assertEquals(endpoint, selection.endpoint)
        assertFalse(selection.isCustom)
    }

    @Test
    @SmallTest
    fun currentUnknownEndpointPinsAsCustomWhenNoEndpointWasTapped() {
        val endpoint = LightWalletEndpoint(host = "custom.example.com", port = 9067, isSecure = true)

        val selection =
            endpoint.toCurrentManualServerSelection(
                persistedSelection = ServerSelection.automatic(),
                availableServers = knownEndpoints
            )

        assertEquals(ConnectionMode.MANUAL, selection.mode)
        assertEquals(endpoint, selection.endpoint)
        assertTrue(selection.isCustom)
    }

    companion object {
        private val knownEndpoints =
            listOf(
                LightWalletEndpoint(host = "zec.rocks", port = 443, isSecure = true),
                LightWalletEndpoint(host = "eu.zec.rocks", port = 443, isSecure = true)
            )
    }
}
