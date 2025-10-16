package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 결제 생성 유스케이스 구현체.
 * - 입력(REST 등) → 도메인/외부PG/영속성 포트를 순차적으로 호출하는 흐름을 담당합니다.
 * - 수수료 정책 조회 및 적용(계산)은 도메인 유틸리티를 통해 수행합니다.
 */
@Service
class PaymentService(
    private val partnerRepository: PartnerOutPort,
    private val feePolicyRepository: FeePolicyOutPort,
    private val paymentRepository: PaymentOutPort,
    private val pgClients: List<PgClientOutPort>,
) : PaymentUseCase {
    /**
     * 결제 승인/수수료 계산/저장을 순차적으로 수행합니다.
     * - 제휴사별 수수료 정책을 조회하여 실제 정책을 적용합니다.
     * - 하드코딩된 수수료 계산을 제거하고 동적 정책 조회로 변경합니다.
     */
    override suspend fun pay(command: PaymentCommand): Payment {

        // 결제를 요청하는 파트너사 객체를 DB에서 조회
        val partner = partnerRepository.findById(command.partnerId)
            ?: throw IllegalArgumentException("Partner not found: ${command.partnerId}")
        require(partner.active) { "Partner is inactive: ${partner.id}" }

        // PgClient 호출 ( 포트-어뎁터 원칙 )
        val pgClient = pgClients.firstOrNull { it.supports(partner.id) }
            ?: throw IllegalStateException("No PG client supports partner: ${partner.id}")

        // 결제 대행사에서 승인 요청 객체를 반환
        val approve = pgClient.approve(
            PgApproveRequest(
                partnerId = partner.id,
                amount = command.amount,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                birthDate = command.birthDate,
                expiry = command.expiry,
                password = command.password,
                productName = command.productName,
            ),
        )

        // 제휴사별 수수료 정책 조회
        val feePolicy = feePolicyRepository.findEffectivePolicy(partner.id)
            ?: throw IllegalStateException("No fee policy found for partner: ${partner.id}")

        // Domain 레이어의 util 비즈니스 로직 호출
        val (fee, net) = FeeCalculator.calculateFee(command.amount, feePolicy.percentage, feePolicy.fixedFee)

        // 결재 이력 스냅샷 객체 반환
        val payment = if (approve.status == PaymentStatus.APPROVED) {
            Payment(
                partnerId = partner.id,
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
                partnerId = partner.id,
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

        // OutBound Port를 호출하여 DB에 저장
        return paymentRepository.save(payment)
    }
}
