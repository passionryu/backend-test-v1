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
import im.bigs.pg.external.pg.TestPgClient
import org.springframework.stereotype.Service


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
     * - 현재 예시 구현은 하드코드된 수수료(3% + 100)로 계산합니다.
     * - 과제: 제휴사별 수수료 정책을 적용하도록 개선해 보세요.
     */
    override fun pay(command: PaymentCommand): Payment {

        // 결제를 요청하는 파트너사 객체를 DB에서 반환
        val partner = partnerRepository.findById(command.partnerId)
            ?: throw IllegalArgumentException("Partner not found: ${command.partnerId}")
        require(partner.active) { "Partner is inactive: ${partner.id}" }

        // TestPgClient 구현체 호출
        val pgClient = pgClients.firstOrNull { it is TestPgClient }
            ?: throw IllegalStateException("No TestPgClient found")

        // 결제 대행사에서 승인 요청 객체를 반환
        val approve = pgClient.approve(
            PgApproveRequest(
                partnerId = partner.id,
                amount = command.amount,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                productName = command.productName,
            ),
        )

        /* 이전 MockPgClient 호출 비즈니스 로직 */
//        // 승인, 정산을 담당하는 결제 대행사 첫번째 객체를 반환 (없을 시 null)
//        val pgClient = pgClients.firstOrNull { it.supports(partner.id) }
//            ?: throw IllegalStateException("No PG client for partner ${partner.id}")
//
//        // 결제 대행사에서 승인 요청 객체를 반환
//        val approve = pgClient.approve(
//            PgApproveRequest(
//                partnerId = partner.id,
//                amount = command.amount,
//                cardBin = command.cardBin,
//                cardLast4 = command.cardLast4,
//                productName = command.productName,
//            ),
//        )

        // 임시 하드코딩
        val hardcodedRate = java.math.BigDecimal("0.0300")
        val hardcodedFixed = java.math.BigDecimal("100")

        // Domain 레이어의 util 비즈니스 로직 호출
        val (fee, net) = FeeCalculator.calculateFee(command.amount, hardcodedRate, hardcodedFixed)

        // 결재 이력 스냅샷 객체 반환
        val payment = Payment(
            partnerId = partner.id,
            amount = command.amount,
            appliedFeeRate = hardcodedRate,
            feeAmount = fee,
            netAmount = net,
            cardBin = command.cardBin,
            cardLast4 = command.cardLast4,
            approvalCode = approve.approvalCode,
            approvedAt = approve.approvedAt,
            status = PaymentStatus.APPROVED,
        )

        // OutBound Port를 호출하여 DB에 저장
        return paymentRepository.save(payment)
    }
}
