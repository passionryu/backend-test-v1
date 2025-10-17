package im.bigs.pg.external.pg.helper

import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.dto.PgApproveApiResponse
import java.time.LocalDateTime

/**
 *  외부 PG 서버 응답을 도메인용 결제 승인 결과로 변환합니다.
 *
 *  @param response 외부 PG 서버에서 받은 응답 DTO. null일 경우 결제 취소 처리됨.
 *  @return 도메인용 PgApproveResult 객체
 */
public fun toApproveResult(response: PgApproveApiResponse?): PgApproveResult {
    return if (response?.status == "APPROVED") {
        PgApproveResult(
            approvalCode = response.approvalCode,
            approvedAt = LocalDateTime.parse(response.approvedAt.substringBefore('.')),
            status = PaymentStatus.APPROVED
        )
    } else {
        PgApproveResult(
            approvalCode = null,
            approvedAt = null,
            status = PaymentStatus.CANCELED,
            failureReason = response?.status ?: "Unknown failure"
        )
    }
}
