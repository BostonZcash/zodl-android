package co.electriccoin.zcash.ui.common.model.near

import co.electriccoin.zcash.ui.common.model.SwapAsset
import co.electriccoin.zcash.ui.common.model.SwapBlockchain
import co.electriccoin.zcash.ui.design.util.ImageResource
import co.electriccoin.zcash.ui.design.util.StringResource

data class NearSwapAsset(
    override val tokenName: StringResource,
    override val tokenIcon: ImageResource,
    override val blockchain: SwapBlockchain,
    private val dto: NearTokenDto,
) : SwapAsset {
    override val tokenTicker = dto.symbol
    override val usdPrice = dto.price
    override val assetId = dto.assetId
    override val decimals = dto.decimals
}