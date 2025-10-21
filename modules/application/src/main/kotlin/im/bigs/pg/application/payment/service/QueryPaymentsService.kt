package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.helper.PaymentCacheKeyHelper
import im.bigs.pg.application.payment.helper.PaymentCursorHelper
import im.bigs.pg.application.payment.helper.PaymentQueryHelper
import im.bigs.pg.application.payment.helper.PaymentStatusMapper
import im.bigs.pg.application.payment.helper.PaymentSummaryHelper
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.domain.payment.PaymentSummary
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 결제 이력 조회 유스케이스 구현체.
 * - 커서 토큰은 createdAt/id를 안전하게 인코딩해 전달/복원합니다.
 * - 통계는 조회 조건과 동일한 집합을 대상으로 계산됩니다.
 */
@Service
class QueryPaymentsService(
    private val paymentRepository: PaymentOutPort,
    private val cursorEncoder: CursorEncoder
) : QueryPaymentsUseCase {

    /**
     * 결제 내역 조회를 순차적으로 수행합니다.
     * Cache-Aside 패턴 적용: 캐시에 없으면 DB 조회 후 캐시에 저장
     *
     * 1. 커서 디코딩 (cursorEncoder.decode)
     * 2. 상태 변환 (String -> PaymentStatus) (PaymentStatusMapper.from)
     * 3. 결제 목록 조회 - 커서 페이지네이션 (PaymentQueryHelper.fetchPayments) - 캐시 적용
     * 4. 통계 조회 - 필터와 동일한 조건으로 조회 (PaymentSummaryHelper.fetchSummary) - 캐시 적용
     * 5. 다음 커서 생성 (PaymentCursorHelper.buildNextCursor)
     * 6. 조회 결과 반환 (QueryResult)
     */
    override fun query(filter: QueryFilter): QueryResult {

        println("1");
        val cursorInfo = cursorEncoder.decode(filter.cursor)
        val paymentStatus = PaymentStatusMapper.from(filter.status)
        println("2");

        val pageResult = PaymentQueryHelper.fetchPayments(paymentRepository, filter, paymentStatus, cursorInfo)
        println("3");
        val summary = PaymentSummaryHelper.fetchSummary(repository = paymentRepository, partnerId = filter.partnerId, status = paymentStatus, from = filter.from, to = filter.to)
        println("4");
        
        // Cache-Aside 패턴: 캐시가 없으면 DB에서 조회 후 캐시에 저장
        //val pageResult = fetchPaymentsWithCache(filter, paymentStatus, cursorInfo)
        //val summary = fetchSummaryWithCache(filter.partnerId, paymentStatus, filter.from, filter.to)

        val nextCursor = PaymentCursorHelper.buildNextCursor(pageResult, cursorEncoder)
        println("5");

        return QueryResult(
            items = pageResult.items,
            summary = PaymentSummary(
                count = summary.count,
                totalAmount = summary.totalAmount,
                totalNetAmount = summary.totalNetAmount,
            ),
            nextCursor = nextCursor,
            hasNext = pageResult.hasNext,
        )
    }

    /**
     * 캐시를 활용한 결제 목록 조회 (Cache-Aside 패턴)
     */
//    @Cacheable(value = ["paymentQueries"], key = "T(im.bigs.pg.application.payment.helper.PaymentCacheKeyHelper).generateQueryCacheKey(#filter, #paymentStatus, #cursorInfo)")
//    private fun fetchPaymentsWithCache(
//        filter: QueryFilter,
//        paymentStatus: im.bigs.pg.domain.payment.PaymentStatus?,
//        cursorInfo: Pair<java.time.LocalDateTime?, Long?>?
//    ) = PaymentQueryHelper.fetchPayments(paymentRepository, filter, paymentStatus, cursorInfo)
//
//    /**
//     * 캐시를 활용한 결제 통계 조회 (Cache-Aside 패턴)
//     */
//    @Cacheable(value = ["paymentSummaries"], key = "T(im.bigs.pg.application.payment.helper.PaymentCacheKeyHelper).generateSummaryCacheKey(#partnerId, #status, #from, #to)")
//    private fun fetchSummaryWithCache(
//        partnerId: Long?,
//        status: im.bigs.pg.domain.payment.PaymentStatus?,
//        from: java.time.LocalDateTime?,
//        to: java.time.LocalDateTime?
//    ) = PaymentSummaryHelper.fetchSummary(paymentRepository, partnerId, status, from, to)

}
