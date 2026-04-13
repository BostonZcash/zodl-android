package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.R
import co.electriccoin.zcash.ui.common.model.LceContent
import co.electriccoin.zcash.ui.common.model.MutableLce
import co.electriccoin.zcash.ui.common.model.stateIn
import co.electriccoin.zcash.ui.design.component.ButtonState
import co.electriccoin.zcash.ui.design.component.ButtonStyle
import co.electriccoin.zcash.ui.design.component.ZashiConfirmationState
import co.electriccoin.zcash.ui.design.util.StringResource
import co.electriccoin.zcash.ui.design.util.stringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ErrorStateMapperUseCase(
    private val sendEmail: SendEmailUseCase,
) {
    operator fun <T> invoke(
        lce: MutableLce<T>,
        scope: CoroutineScope,
        title: StringResource = stringRes(co.electriccoin.zcash.ui.design.R.string.general_error_title),
        message: StringResource = stringRes(co.electriccoin.zcash.ui.design.R.string.general_please_try_again),
        mapper: (LceContent.Error) -> ZashiConfirmationState = { error ->
            ZashiConfirmationState(
                icon = R.drawable.ic_reset_zashi_warning,
                title = title,
                message = message,
                primaryAction =
                    ButtonState(
                        text = stringRes(co.electriccoin.zcash.ui.design.R.string.general_try_again),
                        style = ButtonStyle.DESTRUCTIVE2,
                        onClick = error.restart,
                    ),
                secondaryAction =
                    ButtonState(
                        text = stringRes(co.electriccoin.zcash.ui.design.R.string.general_contact_support),
                        style = ButtonStyle.PRIMARY,
                        onClick = {
                            error.dismiss()
                            scope.launch {
                                sendEmail(error.cause as? Exception ?: Exception(error.cause.message, error.cause))
                            }
                        },
                    ),
                onBack = error.dismiss,
            )
        }
    ): StateFlow<ZashiConfirmationState?> =
        lce.state
            .map { it.error?.let { mapper(it) } }
            .stateIn(scope)
}
