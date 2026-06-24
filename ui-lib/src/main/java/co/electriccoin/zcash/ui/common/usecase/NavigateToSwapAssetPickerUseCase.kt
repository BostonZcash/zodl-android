package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.common.model.SwapAsset
import co.electriccoin.zcash.ui.screen.swap.picker.SwapAssetPickerArgs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class NavigateToSwapAssetPickerUseCase(
    private val navigationRouter: NavigationRouter
) {
    private val pipeline = MutableSharedFlow<SelectSwapAssetPipelineResult>()

    suspend operator fun invoke(onlyChainTicker: String?): SwapAsset? {
        val args = SwapAssetPickerArgs(onlyChainTicker = onlyChainTicker)
        navigationRouter.forward(args)
        val result = pipeline.first { it.args.requestId == args.requestId }
        return when (result) {
            is SelectSwapAssetPipelineResult.Cancelled -> null
            is SelectSwapAssetPipelineResult.Selected -> result.asset
        }
    }

    suspend fun onSelectionCancelled(args: SwapAssetPickerArgs) {
        pipeline.emit(SelectSwapAssetPipelineResult.Cancelled(args))
        navigationRouter.back()
    }

    suspend fun onSelected(asset: SwapAsset, args: SwapAssetPickerArgs) {
        pipeline.emit(SelectSwapAssetPipelineResult.Selected(asset = asset, args = args))
        navigationRouter.back()
    }
}

private sealed interface SelectSwapAssetPipelineResult {
    val args: SwapAssetPickerArgs

    data class Cancelled(
        override val args: SwapAssetPickerArgs
    ) : SelectSwapAssetPipelineResult

    data class Selected(
        val asset: SwapAsset,
        override val args: SwapAssetPickerArgs
    ) : SelectSwapAssetPipelineResult
}
