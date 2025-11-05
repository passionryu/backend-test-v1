package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.helper.PaymentQueryHelper
import im.bigs.pg.application.payment.helper.PaymentSummaryHelper
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 결제 목록 및 통계 조회 시 캐시를 적용하는 서비스.
 *
 * 헥사고널 아키텍처의 애플리케이션 계층에 위치하며,
 * [PaymentOutPort]를 통해 DB에 접근하고 Spring Cache를 사용해
 * 동일한 조회 조건에 대한 중복 조회를 방지한다.
 *
 * Cache-Aside 패턴 적용:
 * - 캐시에 없으면 DB 조회 후 캐시에 저장
 * - 캐시에 있으면 즉시 반환
 */
@Service
class PaymentQueryCacheService(
    private val paymentRepository: PaymentOutPort
) {

    private val logger = LoggerFactory.getLogger(PaymentQueryCacheService::class.java)

    /**
     * 결제 목록을 캐시를 통해 조회한다.
     *
     * 캐시에 데이터가 없을 경우 DB에서 조회 후 캐시에 저장된다.
     *
     * @param filter 결제 조회 필터 (기간, 정렬, partnerId 등)
     * @param paymentStatus 결제 상태 (예: APPROVED, CANCELED 등)
     * @param cursorInfo 커서 기반 페이지네이션 정보 (createdAt, id)
     * @return 결제 목록 페이지 결과
     */
    @Cacheable(
        value = ["paymentQueries"],
        key = "T(im.bigs.pg.application.payment.helper.PaymentCacheKeyHelper).generateQueryCacheKey(#filter, #paymentStatus, #cursorInfo)"
    )
    fun fetchPaymentsWithCache(
        filter: QueryFilter,
        paymentStatus: PaymentStatus?,
        cursorInfo: Pair<java.time.LocalDateTime?, Long?>?
    ): PaymentPage {
        return PaymentQueryHelper.fetchPayments(paymentRepository, filter, paymentStatus, cursorInfo)
    }

    /**
     * 결제 통계(건수, 총 결제금액, 순결제금액)를 캐시를 통해 조회한다.
     *
     * 캐시에 없을 경우 DB에서 직접 계산 후 캐시에 저장하며,
     * 조회 조건(제휴사, 기간, 상태)에 따라 캐시 키가 생성된다.
     *
     * @param partnerId 제휴사(판매자) ID
     * @param status 결제 상태 필터
     * @param from 조회 시작일시
     * @param to 조회 종료일시
     * @return 결제 통계 요약 객체
     */
    @Cacheable(
        value = ["paymentSummaries"],
        key = "T(im.bigs.pg.application.payment.helper.PaymentCacheKeyHelper).generateSummaryCacheKey(#partnerId, #status, #from, #to)"
    )
    fun fetchSummaryWithCache(
        partnerId: Long?,
        status: PaymentStatus?,
        from: java.time.LocalDateTime?,
        to: java.time.LocalDateTime?
    ): PaymentSummary {
        return PaymentSummaryHelper.fetchSummary(paymentRepository, partnerId, status, from, to)
    }
}



