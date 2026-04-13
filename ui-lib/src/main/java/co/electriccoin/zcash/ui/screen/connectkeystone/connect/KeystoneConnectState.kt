package co.electriccoin.zcash.ui.screen.connectkeystone.connect

data class KeystoneConnectState(
    val onViewKeystoneTutorialClicked: () -> Unit,
    val onBackClick: () -> Unit,
    val onContinueClick: () -> Unit,
)
