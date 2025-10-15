package im.bigs.pg.common.pg

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

/** PG 승인 결과 요약. */
data class PgApproveResult(
    val approvalCode: String,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val approvedAt: LocalDateTime,
    val status: PaymentStatus = PaymentStatus.APPROVED,
)
