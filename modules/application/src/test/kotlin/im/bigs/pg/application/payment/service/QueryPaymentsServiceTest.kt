package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QueryPaymentsServiceTest {

    private val paymentRepository = mockk<PaymentOutPort>()
    private val cursorEncoder = mockk<CursorEncoder>()

    private val queryPaymentsService = QueryPaymentsService(
        paymentRepository = paymentRepository,
        cursorEncoder = cursorEncoder
    )

    @Test
    @DisplayName("정상적인 결제 조회 시 모든 단계가 올바르게 실행되어야 한다")
    fun `정상적인 결제 조회 시 모든 단계가 올바르게 실행되어야 한다`() {
        // Given
        val partnerId = 1L
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val cursor = "test-cursor-token"

        val filter = QueryFilter(
            partnerId = partnerId,
            status = "APPROVED",
            from = from,
            to = to,
            cursor = cursor,
            limit = 20
        )

        val cursorInfo = Pair(from.plusDays(1), 100L)
        val paymentStatus = PaymentStatus.APPROVED

        val payments = listOf(
            Payment(
                id = 1L,
                partnerId = partnerId,
                amount = BigDecimal("10000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("235"),
                netAmount = BigDecimal("9765"),
                cardBin = "123456",
                cardLast4 = "7890",
                approvalCode = "APP001",
                approvedAt = LocalDateTime.now(ZoneOffset.UTC),
                status = PaymentStatus.APPROVED
            ),
            Payment(
                id = 2L,
                partnerId = partnerId,
                amount = BigDecimal("20000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("470"),
                netAmount = BigDecimal("19530"),
                cardBin = "654321",
                cardLast4 = "0987",
                approvalCode = "APP002",
                approvedAt = LocalDateTime.now(ZoneOffset.UTC),
                status = PaymentStatus.APPROVED
            )
        )

        val pageResult = PaymentPage(
            items = payments,
            hasNext = true,
            nextCursorCreatedAt = LocalDateTime.now(ZoneOffset.UTC),
            nextCursorId = 200L
        )

        val summaryProjection = PaymentSummaryProjection(
            count = 2L,
            totalAmount = BigDecimal("30000"),
            totalNetAmount = BigDecimal("29295")
        )

        val nextCursorToken = "next-cursor-token"

        // Mock 설정
        every { cursorEncoder.decode(cursor) } returns cursorInfo

        val querySlot = slot<PaymentQuery>()
        every { paymentRepository.findBy(capture(querySlot)) } returns pageResult

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        every { paymentRepository.summary(capture(summaryFilterSlot)) } returns summaryProjection

        every {
            cursorEncoder.encode(any<Instant>(), any<Long>())
        } returns nextCursorToken

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(payments[0].id, result.items[0].id)
        assertEquals(payments[1].id, result.items[1].id)

        assertEquals(2L, result.summary.count)
        assertEquals(BigDecimal("30000"), result.summary.totalAmount)
        assertEquals(BigDecimal("29295"), result.summary.totalNetAmount)

        assertEquals(nextCursorToken, result.nextCursor)
        assertEquals(true, result.hasNext)

        // 검증
        verify { cursorEncoder.decode(cursor) }
        verify { paymentRepository.findBy(any<PaymentQuery>()) }
        verify { paymentRepository.summary(any<PaymentSummaryFilter>()) }

        // PaymentQuery 검증
        val capturedQuery = querySlot.captured
        assertEquals(partnerId, capturedQuery.partnerId)
        assertEquals(PaymentStatus.APPROVED, capturedQuery.status)
        assertEquals(from, capturedQuery.from)
        assertEquals(to, capturedQuery.to)
        assertEquals(20, capturedQuery.limit)
        assertEquals(cursorInfo.first, capturedQuery.cursorCreatedAt)
        assertEquals(cursorInfo.second, capturedQuery.cursorId)

        // PaymentSummaryFilter 검증
        val capturedSummaryFilter = summaryFilterSlot.captured
        assertEquals(partnerId, capturedSummaryFilter.partnerId)
        assertEquals(PaymentStatus.APPROVED, capturedSummaryFilter.status)
        assertEquals(from, capturedSummaryFilter.from)
        assertEquals(to, capturedSummaryFilter.to)
    }


}
