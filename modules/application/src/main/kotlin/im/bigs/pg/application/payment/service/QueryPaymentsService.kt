package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.helper.PaymentQueryHelper
import im.bigs.pg.application.payment.helper.PaymentStatusMapper
import im.bigs.pg.application.payment.helper.PaymentSummaryHelper
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.domain.payment.PaymentSummary
import org.springframework.stereotype.Service
import java.time.ZoneOffset

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
     * 필터를 기반으로 결제 내역을 조회합니다.
     *
     * @param filter 파트너/상태/기간/커서/페이지 크기
     * @return 조회 결과(목록/통계/커서)
     */
    override fun query(filter: QueryFilter): QueryResult {

        val cursorInfo = cursorEncoder.decode(filter.cursor) // 1. 커서 디코딩
        val paymentStatus = PaymentStatusMapper.from(filter.status) // 2. 상태 변환 (String -> PaymentStatus)
        val pageResult = PaymentQueryHelper.fetchPayments(paymentRepository, filter, paymentStatus, cursorInfo) // 3. 결제 목록 조회 (페이지네이션)
        val summary = PaymentSummaryHelper.fetchSummary(repository = paymentRepository, partnerId = filter.partnerId,
            status = paymentStatus, from = filter.from, to = filter.to) // 4. 통계 조회 (필터와 동일한 조건)

        // 커서 생성
        val nextCursor = if (pageResult.hasNext && pageResult.nextCursorCreatedAt != null && pageResult.nextCursorId != null) {
            cursorEncoder.encode(pageResult.nextCursorCreatedAt.toInstant(ZoneOffset.UTC), pageResult.nextCursorId)
        } else null

        // 6. 결과 반환
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

}
