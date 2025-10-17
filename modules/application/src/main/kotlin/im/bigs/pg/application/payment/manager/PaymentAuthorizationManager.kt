package im.bigs.pg.application.payment.manager

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import org.springframework.stereotype.Component

/**
 * 결제 대행사 인증 요청을 담당합니다.
 */
@Component
class PaymentAuthorizationManager {
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
