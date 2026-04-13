package co.electriccoin.zcash.ui.screen.common

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.StyledStringResource

data class EstimatedBlockHeightState(
    val title: StringResource?,
    val subtitle: StringResource,
    val message: StyledStringResource,
    val logo: Int?,
    val blockHeightText: StringResource,
    val onBack: () -> Unit,
    val dialogButton: IconButtonState,
    val copyButton: ButtonState,
    val primaryButton: ButtonState,
)
