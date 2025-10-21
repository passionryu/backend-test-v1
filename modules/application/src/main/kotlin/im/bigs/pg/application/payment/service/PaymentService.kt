package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.manager.FeePolicyManager
import im.bigs.pg.application.partner.manager.PartnerManager
import im.bigs.pg.application.payment.manager.EnhancedPaymentCacheManager
import im.bigs.pg.application.payment.manager.PaymentAuthorizationManager
import im.bigs.pg.application.payment.manager.PaymentBuilder
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.manager.PgClientManager
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.payment.Payment
import org.springframework.stereotype.Service

/**
 * 결제 생성 유스케이스 구현체.
 * - 입력(REST 등) → 도메인/외부PG/영속성 포트를 순차적으로 호출하는 흐름을 담당합니다.
 * - 수수료 정책 조회 및 적용(계산)은 도메인 유틸리티를 통해 수행합니다.
 */
@Service
class PaymentService(
    private val partnerManager: PartnerManager,
    private val pgClientManager: PgClientManager,
    private val paymentAuthorizationManager: PaymentAuthorizationManager,
    private val feePolicyManager: FeePolicyManager,
    private val paymentBuilder: PaymentBuilder,
    private val paymentRepository: PaymentOutPort,
    private val cacheManager: EnhancedPaymentCacheManager
) : PaymentUseCase {

    /**
     * 결제 승인/수수료 계산/저장을 순차적으로 수행합니다.
     * 
     * Cache Eviction: 새 결제 생성 시 해당 제휴사의 캐시된 조회 결과를 무효화
     *
     * 1. Partner 조회 및 활성화 확인 (partnerManager.findPartner)
     * 2. 해당 Partner에 맞는 PG Client 조회 (pgClientManager.findPgClient)
     * 3. PG 승인 요청 및 결과 반환 (paymentAuthorizationManager.authorizePayment)
     * 4. 수수료 정책 조회 및 수수료/정산금 계산 (feePolicyManager.findFeePolicy + FeeCalculator.calculateFee)
     * 5. 결제 객체 생성 (paymentBuilder.buildPayment)
     * 6. DB 저장 (paymentRepository.save)
     * 7. 캐시 무효화 (해당 제휴사의 조회 결과 캐시 삭제)
     */
    override suspend fun pay(command: PaymentCommand): Payment {

        val partner = partnerManager.findPartner(command.partnerId)
        val pgClient = pgClientManager.findPgClient(partner.id)
        val approve = paymentAuthorizationManager.authorizePayment(pgClient, partner.id, command)
        val feePolicy = feePolicyManager.findFeePolicy(partner.id)
        val (fee, net) = FeeCalculator.calculateFee(command.amount, feePolicy.percentage, feePolicy.fixedFee)
        val payment = paymentBuilder.buildPayment(partner.id, command, approve, feePolicy, fee, net)
        val savedPayment = paymentRepository.save(payment)
        cacheManager.evictPartnerCache(command.partnerId)
        return savedPayment
    }
}
