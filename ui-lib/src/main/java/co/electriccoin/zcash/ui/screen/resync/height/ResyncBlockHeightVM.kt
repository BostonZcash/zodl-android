package co.electriccoin.zcash.ui.screen.resync.height

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.common.usecase.ConfirmResyncUseCase
import co.electriccoin.zcash.ui.common.usecase.ShowErrorUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.common.BlockHeightState
import co.electriccoin.zcash.ui.screen.heightinfo.HeightInfoArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResyncBlockHeightVM(
    private val navigationRouter: NavigationRouter,
    private val confirmResync: ConfirmResyncUseCase,
    private val showError: ShowErrorUseCase,
) : ViewModel() {
    private val blockHeightText = MutableStateFlow(NumberTextFieldInnerState())
    private val isConfirming = MutableStateFlow(false)

    val state: StateFlow<BlockHeightState> =
        combine(blockHeightText, isConfirming) { text, isConfirming ->
            createState(text, isConfirming)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            initialValue = createState(blockHeightText.value, false),
        )

    private fun createState(
        blockHeight: NumberTextFieldInnerState,
        isConfirming: Boolean,
    ): BlockHeightState {
        val isHigherThanSaplingActivationHeight =
            blockHeight.amount
                ?.let { it.toLong() >= VersionInfo.NETWORK.saplingActivationHeight.value }
                ?: false
        val isValid = !blockHeight.innerTextFieldState.value.isEmpty() && isHigherThanSaplingActivationHeight

        return BlockHeightState(
            title = stringRes(R.string.resync_title),
            subtitle = stringRes(R.string.resync_wbh_subtitle),
            message = stringRes(R.string.resync_wbh_message),
            logo = null,
            textFieldTitle = stringRes(R.string.restore_bd_text_field_title),
            textFieldHint = stringRes(R.string.restore_bd_text_field_hint),
            textFieldNote = stringRes(R.string.restore_bd_text_field_note),
            onBack = ::onBack,
            dialogButton =
                IconButtonState(
                    icon = R.drawable.ic_help,
                    onClick = ::onInfoClick,
                ),
            primaryButton =
                ButtonState(
                    text = stringRes(R.string.resync_wbh_confirm_button),
                    onClick = ::onConfirmClick,
                    isEnabled = isValid && !isConfirming,
                    isLoading = isConfirming,
                    hapticFeedbackType = HapticFeedbackType.Confirm,
                ),
            secondaryButton = null,
            blockHeight = NumberTextFieldState(innerState = blockHeight, onValueChange = ::onValueChanged),
        )
    }

    private fun onConfirmClick() {
        if (isConfirming.value) return
        val heightValue = blockHeightText.value.amount?.toLong() ?: return
        viewModelScope.launch {
            try {
                isConfirming.update { true }
                confirmResync(BlockHeight.new(heightValue))
            } catch (_: Exception) {
                showError()
            } finally {
                isConfirming.update { false }
            }
        }
    }

    private fun onBack() = navigationRouter.back()

    private fun onInfoClick() = navigationRouter.forward(HeightInfoArgs)

    private fun onValueChanged(state: NumberTextFieldInnerState) = blockHeightText.update { state }
}
