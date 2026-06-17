package co.electriccoin.zcash.ui.common.usecase

import co.electriccoin.zcash.ui.common.model.SubmitResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * gRPC-failure support email content (MOB-1145): a timeout uses dedicated copy, a non-timeout failure
 * uses its description, and the body only appends a detail paragraph when there is non-blank detail.
 */
class SendEmailUseCaseTest {
    @Test
    fun timeoutReasonUsesTimeoutCopy() {
        assertEquals(
            "timeout-copy",
            grpcFailureReportDescription(
                reason = SubmitResult.GrpcFailure.Reason.TIMEOUT,
                description = "ignored-server-text",
                timeoutCopy = { "timeout-copy" }
            )
        )
    }

    @Test
    fun nullReasonUsesDescriptionAndDoesNotResolveTimeoutCopy() {
        var timeoutCopyResolved = false
        val result =
            grpcFailureReportDescription(
                reason = null,
                description = "server detail",
                timeoutCopy = {
                    timeoutCopyResolved = true
                    "timeout-copy"
                }
            )
        assertEquals("server detail", result)
        assertEquals(false, timeoutCopyResolved)
    }

    @Test
    fun nullReasonWithoutDescriptionIsNull() {
        assertNull(
            grpcFailureReportDescription(
                reason = null,
                description = null,
                timeoutCopy = { "timeout-copy" }
            )
        )
    }

    @Test
    fun bodyWithoutDescriptionIsHeaderOnly() {
        assertEquals("Grpc failure\n", buildGrpcFailureEmailBody(null))
        assertEquals("Grpc failure\n", buildGrpcFailureEmailBody("   "))
    }

    @Test
    fun bodyWithDescriptionAppendsParagraph() {
        assertEquals("Grpc failure\n\nserver detail\n", buildGrpcFailureEmailBody("server detail"))
    }
}
