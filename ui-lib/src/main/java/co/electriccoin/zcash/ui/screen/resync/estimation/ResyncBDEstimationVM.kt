package co.electriccoin.zcash.ui.screen.resync.estimation

import android.R.attr.height
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.sdk.ANDROID_STATE_FLOW_TIMEOUT
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.usecase.ConfirmResyncUseCase
import co.electriccoin.zcash.ui.common.usecase.CopyToClipboardUseCase
import co.electriccoin.zcash.ui.common.usecase.GetResyncDataFromHeightUseCase
import co.electriccoin.zcash.ui.common.usecase.ShowErrorUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.util.StringResourceColor
import co.electriccoin.zcash.ui.design.util.StyledStringStyle
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.design.util.stringResByNumber
import co.electriccoin.zcash.ui.design.util.styledStringResource
import co.electriccoin.zcash.ui.design.util.withStyle
import co.electriccoin.zcash.ui.screen.restore.estimation.RestoreBDEstimationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class ResyncBDEstimationVM(
    private val args: ResyncBDEstimationArgs,
    private val navigationRouter: NavigationRouter,
    private val copyToClipboard: CopyToClipboardUseCase,
    private val getResyncDataFromHeight: GetResyncDataFromHeightUseCase,
    private val confirmResync: ConfirmResyncUseCase,
    private val showError: ShowErrorUseCase,
) : ViewModel() {
    private val yearMonthFlow: Flow<YearMonth> =
        flow {
            emit(getResyncDataFromHeight(BlockHeight.new(args.blockHeight)))
        }

    val state: StateFlow<RestoreBDEstimationState?> =
        yearMonthFlow
            .map { createState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
                initialValue = null
            )

    private fun createState(yearMonth: YearMonth): RestoreBDEstimationState =
        RestoreBDEstimationState(
            title = stringRes(R.string.resync_title),
            subtitle = stringRes(R.string.resync_bd_estimation_subtitle),
            message =
                styledStringResource(
                    resource = R.string.resync_bd_estimation_message,
                    style =
                        StyledStringStyle(
                            color = StringResourceColor.TERTIARY,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        ),
                    stringRes(yearMonth).withStyle(
                        StyledStringStyle(
                            color = StringResourceColor.PRIMARY,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    ),
                    stringResByNumber(args.blockHeight, 0).withStyle(
                        style =
                            StyledStringStyle(
                                color = StringResourceColor.TERTIARY
                            )
                    )
                ),
            dialogButton = null,
            onBack = ::onBack,
            text = stringResByNumber(args.blockHeight, 0),
            copy =
                ButtonState(
                    text = stringRes(R.string.restore_bd_estimation_copy),
                    icon = R.drawable.ic_copy,
                    onClick = ::onCopyClick
                ),
            restore =
                ButtonState(
                    text = stringRes(R.string.confirm_resync_confirm),
                    onClick = ::onConfirmClick,
                ),
        )

    private fun onCopyClick() {
        copyToClipboard(
            value = args.blockHeight.toString()
        )
    }

    private fun onConfirmClick() {
        viewModelScope.launch {
            try {
                confirmResync(BlockHeight.new(args.blockHeight))
            } catch (_: Exception) {
                showError()
            }
        }
    }

    private fun onBack() = navigationRouter.back()
}
