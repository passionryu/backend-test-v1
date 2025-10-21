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

/**
 * 결제 객체(Payment) 생성을 담당하는 빌더
 * - 승인 결과와 정책에 따라 Payment 도메인 객체 생성
 */
@Component
class PaymentBuilder {

    /**
     * 결제 객체를 생성합니다.
     *
     * @param partnerId 제휴사 ID
     * @param command 결제 요청 Command
     * @param approve PG 승인 결과
     * @param feePolicy 수수료 정책
     * @param fee 계산된 수수료 금액
     * @param net 계산된 정산 금액
     * @return Payment 도메인 객체
     */
    fun buildPayment(
        partnerId: Long,
        command: PaymentCommand,
        approve: PgApproveResult,
        feePolicy: FeePolicy,
        fee: BigDecimal,
        net: BigDecimal
    ): Payment {
        return if (approve.status == PaymentStatus.APPROVED) {
            // 승인 성공 시
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
            // 승인 실패 시: 취소 처리
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
                failedAt = LocalDateTime.now(ZoneOffset.UTC) // 실패 시점 기록
            )
        }
    }
}

