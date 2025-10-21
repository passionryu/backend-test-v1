package im.bigs.pg.application.payment.manager

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import org.springframework.stereotype.Component

/**
 * 결제 대행사(PG) 인증 요청을 담당하는 매니저
 * - PG 승인 API 호출을 캡슐화
 */
@Component
class PaymentAuthorizationManager {

    /**
     * PG Client를 통해 결제를 승인합니다.
     *
     * @param pgClient 결제 대행사 클라이언트
     * @param partnerId 제휴사 ID
     * @param command 결제 요청 Command
     * @return PG 승인 결과(PgApproveResult)
     */
    suspend fun authorizePayment(pgClient: PgClientOutPort, partnerId: Long, command: PaymentCommand): PgApproveResult {
        return pgClient.approve(
            PgApproveRequest(
                partnerId = partnerId,
                amount = command.amount,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                birthDate = command.birthDate,
                expiry = command.expiry,
                password = command.password,
                productName = command.productName
            )
        )
    }
}
