package co.electriccoin.zcash.ui.screen.restore.tor

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.CheckboxState
import co.electriccoin.zcash.ui.design.component.ModalBottomSheetState

data class RestoreTorState(
    val checkbox: CheckboxState,
    val primary: ButtonState,
    val secondary: ButtonState,
    override val onBack: () -> Unit
) : ModalBottomSheetState
