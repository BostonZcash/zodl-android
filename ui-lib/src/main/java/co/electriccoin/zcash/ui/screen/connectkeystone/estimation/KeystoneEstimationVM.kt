package co.electriccoin.zcash.ui.screen.connectkeystone.estimation

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.zcash.ui.NavigationRouter
import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.guardLoading
import co.electriccoin.zcash.ui.common.model.mutableLce
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.common.usecase.CreateKeystoneAccountUseCase
import co.electriccoin.zcash.ui.common.usecase.ErrorStateMapperUseCase
import co.electriccoin.zcash.ui.common.usecase.GetResyncDataFromHeightUseCase
import co.electriccoin.zcash.ui.common.usecase.ParseKeystoneUrToZashiAccountsUseCase
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.IconButtonState
import co.electriccoin.zcash.ui.design.util.StringResourceColor
import co.electriccoin.zcash.ui.design.util.StyledStringStyle
import co.electriccoin.zcash.ui.design.util.stringRes
import co.electriccoin.zcash.ui.design.util.stringResByNumber
import co.electriccoin.zcash.ui.design.util.styledStringResource
import co.electriccoin.zcash.ui.design.util.withStyle
import co.electriccoin.zcash.ui.screen.common.EstimatedBlockHeightState
import co.electriccoin.zcash.ui.screen.heightinfo.HeightInfoArgs
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.YearMonth

class KeystoneEstimationVM(
    private val args: KeystoneEstimationArgs,
    parseKeystoneUrToZashiAccounts: ParseKeystoneUrToZashiAccountsUseCase,
    private val getResyncDataFromHeight: GetResyncDataFromHeightUseCase,
    private val createKeystoneAccount: CreateKeystoneAccountUseCase,
    private val errorStateMapper: ErrorStateMapperUseCase,
    private val navigationRouter: NavigationRouter,
) : ViewModel() {
    private val accounts = parseKeystoneUrToZashiAccounts(args.ur)
    private val createAccountLce = mutableLce<Unit>()

    private val yearMonthFlow =
        flow {
            emit(getResyncDataFromHeight(BlockHeight.new(args.blockHeight)))
        }

    val errorState = errorStateMapper(createAccountLce, viewModelScope)

    val state =
        combine(yearMonthFlow, createAccountLce.state) { yearMonth, lce ->
            createState(yearMonth = yearMonth, isLoading = lce.loading)
        }.stateIn(this)

    private fun createState(
        yearMonth: YearMonth,
        isLoading: Boolean,
    ) = EstimatedBlockHeightState(
        title = null,
        subtitle = stringRes(R.string.keystone_first_transaction_estimation_subtitle),
        message =
            styledStringResource(
                resource = R.string.keystone_first_transaction_estimation_message,
                style =
                    StyledStringStyle(
                        color = StringResourceColor.TERTIARY,
                        fontWeight = FontWeight.Medium,
                    ),
                stringRes(yearMonth).withStyle(
                    StyledStringStyle(
                        color = StringResourceColor.PRIMARY,
                        fontWeight = FontWeight.SemiBold,
                    )
                ),
                stringResByNumber(args.blockHeight, 0).withStyle(
                    StyledStringStyle(color = StringResourceColor.TERTIARY)
                ),
            ),
        logo = co.electriccoin.zcash.ui.design.R.drawable.image_keystone,
        dialogButton =
            IconButtonState(
                icon = R.drawable.ic_help,
                onClick = ::onInfoClick,
            ),
        onBack = ::onBack,
        blockHeightText = stringResByNumber(args.blockHeight, 0),
        copyButton =
            ButtonState(
                text = stringRes(R.string.restore_bd_estimation_copy),
                icon = R.drawable.ic_copy,
                onClick = {},
            ),
        primaryButton =
            ButtonState(
                text = stringRes(R.string.keystone_first_transaction_estimation_confirm),
                isLoading = isLoading,
                onClick = ::onConfirmClick,
                hapticFeedbackType = HapticFeedbackType.Confirm,
            ),
    )

    private fun onConfirmClick() =
        createAccountLce.execute {
            val account = accounts.accounts.first()
            createKeystoneAccount(accounts, account, BlockHeight.new(args.blockHeight))
        }

    private fun onInfoClick() = navigationRouter.forward(HeightInfoArgs)

    private fun onBack() = createAccountLce.guardLoading { navigationRouter.back() }
}
