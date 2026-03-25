package co.electriccoin.zcash.ui.screen.advancedsettings

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.listitem.ListItemState

data class AdvancedSettingsState(
    val onBack: () -> Unit,
    val items: List<ListItemState>,
    val deleteButton: ButtonState,
)
