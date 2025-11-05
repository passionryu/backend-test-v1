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

@Service
class PaymentQueryCacheService(
    private val paymentRepository: PaymentOutPort
) {

    private val logger = LoggerFactory.getLogger(PaymentQueryCacheService::class.java)

    @Cacheable(
        value = ["paymentQueries"],
        key = "T(im.bigs.pg.application.payment.helper.PaymentCacheKeyHelper).generateQueryCacheKey(#filter, #paymentStatus, #cursorInfo)"
    )
    fun fetchPaymentsWithCache(
        filter: QueryFilter,
        paymentStatus: PaymentStatus?,
        cursorInfo: Pair<java.time.LocalDateTime?, Long?>?
    ): PaymentPage {
        val startTime = System.currentTimeMillis()
        val result = PaymentQueryHelper.fetchPayments(paymentRepository, filter, paymentStatus, cursorInfo)
        val endTime = System.currentTimeMillis()

        logger.info("[CACHE] 결제 목록 조회 완료 - 실행시간: ${endTime - startTime}ms, 결과건수: ${result.items.size}")
        return result
    }

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
        val startTime = System.currentTimeMillis()
        val result = PaymentSummaryHelper.fetchSummary(paymentRepository, partnerId, status, from, to)
        val endTime = System.currentTimeMillis()

        logger.info("[CACHE] 결제 통계 조회 완료 - 실행시간: ${endTime - startTime}ms, count: ${result.count}, totalAmount: ${result.totalAmount}")
        return result
    }
}



