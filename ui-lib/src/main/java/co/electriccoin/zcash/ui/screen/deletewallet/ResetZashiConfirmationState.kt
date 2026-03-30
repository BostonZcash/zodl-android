package co.electriccoin.zcash.ui.screen.deletewallet

import co.electriccoin.zcash.ui.design.component.ModalBottomSheetState

data class ResetZashiConfirmationState(
    override val onBack: () -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
) : ModalBottomSheetState
