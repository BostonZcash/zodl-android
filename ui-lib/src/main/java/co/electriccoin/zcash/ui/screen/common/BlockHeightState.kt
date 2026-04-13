package co.electriccoin.zcash.ui.screen.common

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.component.NumberTextFieldState
import co.electriccoin.zcash.ui.design.util.StringResource

data class BlockHeightState(
    val title: StringResource?,
    val subtitle: StringResource,
    val message: StringResource,
    val logo: Int?,
    val textFieldTitle: StringResource,
    val textFieldHint: StringResource,
    val textFieldNote: StringResource,
    val blockHeight: NumberTextFieldState,
    val primaryButton: ButtonState,
    val secondaryButton: ButtonState?,
    val dialogButton: IconButtonState?,
    val onBack: () -> Unit,
)
