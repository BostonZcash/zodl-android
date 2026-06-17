package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.model.SubmitResult
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Shielding outcome routing (MOB-1145): shielding has no pending screen, so every non-success
 * result routes to the error screen - including Partial (which must not be silently ignored) and a
 * resubmittable GrpcFailure.
 */
class ShieldFundsUseCaseTest {
    @Test
    fun successIsNotAnError() {
        assertEquals(false, SubmitResult.Success(txIds = listOf("a")).isShieldingError())
    }

    @Test
    fun failureIsAnError() {
        assertEquals(
            true,
            SubmitResult.Failure(txIds = listOf("a"), code = 18, description = "rejected").isShieldingError()
        )
    }

    @Test
    fun grpcFailureIsAnError() {
        assertEquals(true, SubmitResult.GrpcFailure(txIds = listOf("a")).isShieldingError())
    }

    @Test
    fun errorIsAnError() {
        assertEquals(true, SubmitResult.Error(cause = RuntimeException("boom")).isShieldingError())
    }

    @Test
    fun partialIsAnError() {
        // Regression guard: a partial shielding submission must surface as an error, not be dropped.
        assertEquals(
            true,
            SubmitResult
                .Partial(txIds = listOf("a", "b"), statuses = listOf("success", "notAttempted"))
                .isShieldingError()
        )
    }
}
