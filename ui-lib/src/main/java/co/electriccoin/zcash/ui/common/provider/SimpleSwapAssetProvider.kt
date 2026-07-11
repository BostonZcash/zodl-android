package co.electriccoin.zcash.ui.common.provider

import co.electriccoin.zcash.ui.common.model.DynamicSimpleSwapAsset
import co.electriccoin.zcash.ui.common.model.SimpleSwapAsset
import co.electriccoin.zcash.ui.common.model.ZecSimpleSwapAsset

interface SimpleSwapAssetProvider {
    fun get(tokenTicker: String, chainTicker: String): SimpleSwapAsset

    fun getCuratedSwapAssets(): List<SimpleSwapAsset>
}

class SimpleSwapAssetProviderImpl(
    private val tokenIconProvider: TokenIconProvider,
    private val tokenNameProvider: TokenNameProvider,
    private val blockchainProvider: BlockchainProvider,
) : SimpleSwapAssetProvider {
    private val allowedAssets: List<Pair<String, String>>
        get() =
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

    override fun get(tokenTicker: String, chainTicker: String): SimpleSwapAsset =
        if (tokenTicker.lowercase() == "zec" && chainTicker.lowercase() == "zec") {
            ZecSimpleSwapAsset(
                tokenName = tokenNameProvider.getName(tokenTicker),
                tokenIcon = tokenIconProvider.getIcon(tokenTicker),
                blockchain = blockchainProvider.getBlockchain(chainTicker),
                tokenTicker = tokenTicker,
            )
        } else {
            DynamicSimpleSwapAsset(
                tokenName = tokenNameProvider.getName(tokenTicker),
                tokenIcon = tokenIconProvider.getIcon(tokenTicker),
                blockchain = blockchainProvider.getBlockchain(chainTicker),
                tokenTicker = tokenTicker,
            )
        }

    override fun getCuratedSwapAssets(): List<SimpleSwapAsset> =
        allowedAssets.map { (tokenTicker, chainTicker) -> get(tokenTicker = tokenTicker, chainTicker = chainTicker) }
}
