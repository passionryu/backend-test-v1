package im.bigs.pg.application.payment.manager

import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.partner.FeePolicy
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PaymentBuilder {
    fun buildPayment(
        partnerId: Long,
        command: PaymentCommand,
        approve: PgApproveResult,
        feePolicy: FeePolicy,
        fee: BigDecimal,
        net: BigDecimal
    ): Payment {
        return if (approve.status == PaymentStatus.APPROVED) {
            Payment(
                partnerId = partnerId,
                amount = command.amount,
                appliedFeeRate = feePolicy.percentage,
                feeAmount = fee,
                netAmount = net,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                approvalCode = approve.approvalCode,
                approvedAt = approve.approvedAt,
                status = PaymentStatus.APPROVED
            )
        } else {
            Payment(
                partnerId = partnerId,
                amount = command.amount,
                appliedFeeRate = feePolicy.percentage,
                feeAmount = fee,
                netAmount = net,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                status = PaymentStatus.CANCELED,
                canceledReason = approve.failureReason,
                failedAt = LocalDateTime.now(ZoneOffset.UTC)
            )
        }
    }
}

