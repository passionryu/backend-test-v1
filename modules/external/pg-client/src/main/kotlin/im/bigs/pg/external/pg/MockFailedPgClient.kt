package im.bigs.pg.external.pg

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Component

@Component
class MockFailedPgClient : PgClientOutPort {

    /**
     * 해당 구현체는 실패 테스트 시에만 활성화
     * - Boolean = true : 활성화
     * - Boolean = false : 비활성화
     */
    override fun supports(partnerId: Long) = false

    override fun approve(request: PgApproveRequest) = PgApproveResult(
        approvalCode = null,
        approvedAt = null,
        status = PaymentStatus.CANCELED,
        failureReason = "Card expired"
    )
}
