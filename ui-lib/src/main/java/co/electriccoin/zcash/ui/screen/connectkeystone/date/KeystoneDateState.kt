package co.electriccoin.zcash.ui.screen.connectkeystone.date

import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResource
import java.time.YearMonth

data class KeystoneDateState(
    val title: StringResource,
    val subtitle: StringResource,
    val message: StringResource,
    val note: StringResource,
    val selection: YearMonth,
    val next: ButtonState,
    val enterBlockHeight: ButtonState,
    val onBack: () -> Unit,
    val onInfo: () -> Unit,
    val onYearMonthChange: (YearMonth) -> Unit,
)
