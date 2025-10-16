package im.bigs.pg.common

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

/** PG 승인 결과 요약. */
data class PgApproveResult(
    val approvalCode: String? = null,        // 실패 시 null 허용
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val approvedAt: LocalDateTime? = null,
    val status: PaymentStatus,
    val failureReason: String? = null // 실패/취소 사유
)
