package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.model.isSame
import co.electriccoin.zcash.ui.common.provider.SimpleSwapAssetProvider
import co.electriccoin.zcash.ui.common.repository.SwapAssetsData
import co.electriccoin.zcash.ui.common.repository.SwapRepository
import kotlinx.coroutines.flow.map

class GetCuratedSwapAssetsUseCase(
    private val swapRepository: SwapRepository,
    private val simpleSwapAssetProvider: SimpleSwapAssetProvider,
) {
    operator fun invoke() = curate(swapRepository.assets.value)

    fun observe() = swapRepository.assets.map(::curate)

    private fun curate(data: SwapAssetsData): SwapAssetsData {
        val inclusionList = simpleSwapAssetProvider.getCuratedSwapAssets()
        return data.copy(
            data =
                data.data
                    ?.filter { token ->
                        inclusionList.any {
                            it.isSame(token.tokenTicker, token.blockchain.chainTicker)
                        }
                    }
        )
    }
}
