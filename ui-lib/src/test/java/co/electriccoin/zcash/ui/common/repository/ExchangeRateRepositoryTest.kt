package co.electriccoin.zcash.ui.common.repository

import cash.z.ecc.android.sdk.model.FiatCurrency
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the synchronizer-route fallback decision used by [ExchangeRateRepositoryImpl] when the
 * CMC exchange-rate lookup fails or is unavailable (MOB-1124).
 */
class ExchangeRateRepositoryTest {
    @Test
    fun fallsBackWhenCmcUnavailableAndFiatIsUsd() {
        assertTrue(
            shouldFallBackToSynchronizerRoute(isCmcAvailable = false, fiat = FiatCurrency.USD)
        )
    }

    @Test
    fun fallsBackWhenCmcUnavailableAndFiatIsNonUsd() {
        // CMC is the only rate source when unavailable, so the USD-only synchronizer route is used
        // regardless of the preferred fiat (e.g. a stale non-USD preference from a prior build).
        assertTrue(
            shouldFallBackToSynchronizerRoute(isCmcAvailable = false, fiat = FiatCurrency("EUR"))
        )
    }

    @Test
    fun fallsBackWhenCmcAvailableAndFiatIsUsd() {
        assertTrue(
            shouldFallBackToSynchronizerRoute(isCmcAvailable = true, fiat = FiatCurrency.USD)
        )
    }

    @Test
    fun doesNotFallBackWhenCmcAvailableAndFiatIsNonUsd() {
        // The synchronizer route only provides a USD rate, so it cannot serve a non-USD fiat.
        assertFalse(
            shouldFallBackToSynchronizerRoute(isCmcAvailable = true, fiat = FiatCurrency("EUR"))
        )
    }
}
