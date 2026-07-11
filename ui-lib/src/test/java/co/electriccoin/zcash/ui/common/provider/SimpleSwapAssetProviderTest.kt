package co.electriccoin.zcash.ui.common.provider

import co.electriccoin.zcash.ui.common.model.SwapBlockchain
import co.electriccoin.zcash.ui.common.model.isSame
import co.electriccoin.zcash.ui.design.util.imageRes
import co.electriccoin.zcash.ui.design.util.stringRes
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleSwapAssetProviderTest {
    private val provider =
        SimpleSwapAssetProviderImpl(
            tokenIconProvider =
                mockk {
                    every { getIcon(any()) } answers { imageRes(firstArg<String>()) }
                },
            tokenNameProvider =
                mockk {
                    every { getName(any()) } answers { stringRes(firstArg<String>()) }
                },
            blockchainProvider =
                mockk {
                    every { getBlockchain(any()) } answers {
                        val ticker = firstArg<String>()
                        SwapBlockchain(chainTicker = ticker, chainName = stringRes(ticker), chainIcon = imageRes(ticker))
                    }
                },
        )

    @Test
    fun inclusionListContainsAllCuratedAssets() {
        val curated =
            listOf(
                "ZEC" to "zec",
                "USDC" to "eth",
                "USDT" to "tron",
                "USDC" to "sol",
                "USDT" to "eth",
                "BTC" to "btc",
                "ETH" to "eth",
                "SOL" to "sol",
                "USDT" to "bsc",
                "USDC" to "base",
                "USDT" to "sol",
                "USDC" to "arb",
                "USDC" to "sui",
                "wNEAR" to "near",
                "USDC" to "near",
            )

        val result = provider.getCuratedSwapAssets()

        assertEquals(curated.size, result.size)
        curated.forEach { (symbol, blockchain) ->
            assertTrue(result.any { it.isSame(symbol, blockchain) }, "$symbol@$blockchain should be included")
        }
    }

    @Test
    fun inclusionListExcludesAssetNotInAllowList() {
        val result = provider.getCuratedSwapAssets()

        assertTrue(result.none { it.isSame("DOGE", "doge") })
    }

    @Test
    fun inclusionListExcludesRightSymbolOnWrongChain() {
        val result = provider.getCuratedSwapAssets()

        assertTrue(result.none { it.isSame("USDC", "tron") })
    }

    @Test
    fun inclusionListIsCaseInsensitiveForLookup() {
        val result = provider.getCuratedSwapAssets()

        assertTrue(result.any { it.isSame("zec", "ZEC") })
    }
}
