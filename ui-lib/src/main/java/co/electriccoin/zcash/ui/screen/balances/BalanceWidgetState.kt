package co.electriccoin.zcash.ui.screen.balances

import cash.z.ecc.android.sdk.model.Zatoshi
import co.electriccoin.zcash.ui.common.wallet.ExchangeRateState

data class BalanceWidgetState(
    val showDust: Boolean,
    val totalBalance: Zatoshi,
    val button: BalanceButtonState?,
    val exchangeRate: ExchangeRateState?,
)
