package co.electriccoin.zcash.ui.common.model.near

import co.electriccoin.zcash.ui.common.model.DynamicSimpleSwapAsset
import co.electriccoin.zcash.ui.common.model.DynamicSwapAsset
import co.electriccoin.zcash.ui.common.model.SwapBlockchain
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.imageRes
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Unit tests for the pure NEAR swap quote validators introduced for the swap-security hardening
 * (MOB-1340: Z2 slippage fail-closed, Z3 asset substitution, Z4 amount consistency).
 */
class NearSwapQuoteValidationTest {
    // region requireConsistent — Z4: signed raw amount must equal the displayed formatted amount

    @Test
    fun requireConsistent_passesWhenRawMatchesFormattedShiftedByDecimals() {
        // 1.0 token at 8 decimals == 100_000_000 base units
        requireConsistent(name = "amountIn", raw = BigDecimal("100000000"), formatted = BigDecimal("1"), decimals = 8)
    }

    @Test
    fun requireConsistent_passesRegardlessOfFormattedScale() {
        requireConsistent(name = "amountIn", raw = BigDecimal("100000000"), formatted = BigDecimal("1.00"), decimals = 8)
    }

    @Test
    fun requireConsistent_throwsWhenRawDoesNotMatchFormatted() {
        assertFailsWith<IllegalArgumentException> {
            // formatted=2 -> expects 200_000_000 base units, raw says 100_000_000
            requireConsistent(name = "amountIn", raw = BigDecimal("100000000"), formatted = BigDecimal("2"), decimals = 8)
        }
    }

    @Test
    fun requireConsistent_noOpWhenEitherValueNull() {
        requireConsistent(name = "amountIn", raw = null, formatted = BigDecimal("1"), decimals = 8)
        requireConsistent(name = "amountIn", raw = BigDecimal("100000000"), formatted = null, decimals = 8)
    }

    // endregion

    // region requireWithinSlippage — Z2: server's worst-case guarantee must respect requested slippage

    @Test
    fun requireWithinSlippage_outputFloating_passesAtAndAboveFloor() {
        // 10% slippage, amountOut=100 -> floor=90
        requireWithinSlippage(SwapType.EXACT_INPUT, A_THOUSAND, BigDecimal("100"), null, BigDecimal("90"), BPS_10_PCT)
        requireWithinSlippage(SwapType.EXACT_INPUT, A_THOUSAND, BigDecimal("100"), null, BigDecimal("95"), BPS_10_PCT)
        requireWithinSlippage(SwapType.FLEX_INPUT, A_THOUSAND, BigDecimal("100"), null, BigDecimal("90"), BPS_10_PCT)
    }

    @Test
    fun requireWithinSlippage_outputFloating_throwsBelowFloor() {
        assertFailsWith<IllegalArgumentException> {
            // floor=90, server only guarantees 89
            requireWithinSlippage(SwapType.EXACT_INPUT, A_THOUSAND, BigDecimal("100"), null, BigDecimal("89"), BPS_10_PCT)
        }
    }

    @Test
    fun requireWithinSlippage_inputFloating_passesAtAndBelowCeiling() {
        // 10% slippage, amountIn=100 -> ceiling=110
        requireWithinSlippage(SwapType.EXACT_OUTPUT, BigDecimal("100"), A_THOUSAND, BigDecimal("110"), null, BPS_10_PCT)
        requireWithinSlippage(SwapType.EXACT_OUTPUT, BigDecimal("100"), A_THOUSAND, BigDecimal("105"), null, BPS_10_PCT)
    }

    @Test
    fun requireWithinSlippage_inputFloating_throwsAboveCeiling() {
        assertFailsWith<IllegalArgumentException> {
            // ceiling=110, server demands at least 111
            requireWithinSlippage(SwapType.EXACT_OUTPUT, BigDecimal("100"), A_THOUSAND, BigDecimal("111"), null, BPS_10_PCT)
        }
    }

    @Test
    fun requireWithinSlippage_noOpWhenBoundAbsent() {
        // Server omitted min* — defense-in-depth only, must not reject (see QuoteDetails nullability note).
        requireWithinSlippage(SwapType.EXACT_INPUT, A_THOUSAND, BigDecimal("100"), null, null, BPS_10_PCT)
        requireWithinSlippage(SwapType.EXACT_OUTPUT, BigDecimal("100"), A_THOUSAND, null, null, BPS_10_PCT)
    }

    @Test
    fun requireWithinSlippage_noOpWhenSwapTypeNull() {
        requireWithinSlippage(null, BigDecimal("100"), BigDecimal("100"), BigDecimal("1"), BigDecimal("1"), BPS_10_PCT)
    }

    // endregion

    // region requireMatchingAsset — Z3: returned asset must match the expected asset (ticker + chain)

    @Test
    fun requireMatchingAsset_passesOnSameTickerAndChainCaseInsensitive() {
        requireMatchingAsset(
            name = "originAsset",
            expected = simpleAsset(token = "btc", chain = "bitcoin"),
            actual = asset(token = "BTC", chain = "BITCOIN")
        )
    }

    @Test
    fun requireMatchingAsset_throwsOnDifferentToken() {
        assertFailsWith<IllegalArgumentException> {
            requireMatchingAsset(
                name = "originAsset",
                expected = simpleAsset(token = "BTC", chain = "Bitcoin"),
                actual = asset(token = "ETH", chain = "Bitcoin")
            )
        }
    }

    @Test
    fun requireMatchingAsset_throwsOnDifferentChain() {
        assertFailsWith<IllegalArgumentException> {
            requireMatchingAsset(
                name = "destinationAsset",
                expected = simpleAsset(token = "USDC", chain = "Ethereum"),
                actual = asset(token = "USDC", chain = "Polygon")
            )
        }
    }

    // endregion

    private fun blockchain(chain: String) =
        SwapBlockchain(chainTicker = chain, chainName = StringResource.ByString(chain), chainIcon = imageRes(chain))

    private fun asset(token: String, chain: String) =
        DynamicSwapAsset(
            tokenTicker = token,
            tokenName = StringResource.ByString(token),
            tokenIcon = imageRes(token),
            usdPrice = null,
            assetId = "$token.$chain",
            decimals = 8,
            blockchain = blockchain(chain)
        )

    private fun simpleAsset(token: String, chain: String) =
        DynamicSimpleSwapAsset(
            tokenTicker = token,
            tokenName = StringResource.ByString(token),
            tokenIcon = imageRes(token),
            blockchain = blockchain(chain)
        )

    private companion object {
        val A_THOUSAND: BigDecimal = BigDecimal("1000")
        const val BPS_10_PCT = 1000
    }
}
