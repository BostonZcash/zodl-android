package co.electriccoin.zcash.ui.screen.connectkeystone.height

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.VersionInfo
import co.electriccoin.zcash.ui.common.model.guardLoading
import co.electriccoin.zcash.ui.common.model.mutableLce
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.usecase.CreateKeystoneAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.ErrorStateMapperUseCase
import co.electriccoin.zcash.ui.common.usecase.ParseKeystoneUrToZashiAccountsUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldInnerState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.screen.common.BlockHeightState
import co.electriccoin.zcash.ui.screen.heightinfo.HeightInfoArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class KeystoneHeightVM(
    private val args: KeystoneHeightArgs,
    parseKeystoneUrToZashiAccounts: ParseKeystoneUrToZashiAccountsUseCase,
    private val createKeystoneAccount: CreateKeystoneAccountUseCase,
    private val errorStateMapper: ErrorStateMapperUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val accounts = parseKeystoneUrToZashiAccounts(args.ur)
    private val account = accounts.accounts.firstOrNull()
    private val blockHeightText = MutableStateFlow(NumberTextFieldInnerState())
    private val createAccountLce = mutableLce<Unit>()

    val errorState = errorStateMapper(createAccountLce, viewModelScope)

    val state =
        combine(blockHeightText, createAccountLce.state) { text, lce ->
            val isHigherThanSaplingActivationHeight =
                text.amount
                    ?.let { it.toLong() >= VersionInfo.NETWORK.saplingActivationHeight.value }
                    ?: false
            val isValid = !text.innerTextFieldState.value.isEmpty() && isHigherThanSaplingActivationHeight

            BlockHeightState(
                title = null,
                subtitle = stringRes(R.string.keystone_wbh_subtitle),
                message = stringRes(R.string.keystone_wbh_message),
                logo = co.electriccoin.zcash.ui.design.R.drawable.image_keystone,
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
                        text = stringRes(R.string.keystone_wbh_confirm_button),
                        onClick = { text.amount?.toLong()?.let { onConfirmClick(it) } },
                        isEnabled = isValid && !lce.loading,
                        isLoading = lce.loading,
                        hapticFeedbackType = HapticFeedbackType.Confirm,
                    ),
                secondaryButton = null,
                blockHeight = NumberTextFieldState(innerState = text, onValueChange = ::onValueChanged),
            )
        }.stateIn(this)

    private fun onConfirmClick(height: Long) {
        createAccountLce.execute {
            createKeystoneAccount(
                accounts,
                account ?: throw InitializeException.NoAccountLoaded,
                BlockHeight.new(height),
            )
        }
    }

    private fun onInfoClick() = navigationRouter.forward(HeightInfoArgs)

    private fun onBack() = createAccountLce.guardLoading { navigationRouter.back() }

    private fun onValueChanged(state: NumberTextFieldInnerState) = blockHeightText.update { state }
}
