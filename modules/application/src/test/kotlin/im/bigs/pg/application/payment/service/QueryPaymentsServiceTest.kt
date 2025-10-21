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


    @Test
    @DisplayName("커서가 없는 경우 null 커서 정보로 조회해야 한다")
    fun `커서가 없는 경우 null 커서 정보로 조회해야 한다`() {
        // Given
        val filter = QueryFilter(
            partnerId = 1L,
            status = "CANCELED",
            cursor = null,
            limit = 10
        )

        val emptyPayments = emptyList<Payment>()
        val pageResult = PaymentPage(
            items = emptyPayments,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryProjection = PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO
        )

        // Mock 설정
        every { cursorEncoder.decode(null) } returns null
        every { paymentRepository.findBy(any<PaymentQuery>()) } returns pageResult
        every { paymentRepository.summary(any<PaymentSummaryFilter>()) } returns summaryProjection

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertNotNull(result)
        assertEquals(0, result.items.size)
        assertEquals(0L, result.summary.count)
        assertNull(result.nextCursor)
        assertEquals(false, result.hasNext)

        // 검증
        verify { cursorEncoder.decode(null) }

        val querySlot = slot<PaymentQuery>()
        verify { paymentRepository.findBy(capture(querySlot)) }
        assertEquals(null, querySlot.captured.cursorCreatedAt)
        assertEquals(null, querySlot.captured.cursorId)
    }

    @Test
    @DisplayName("상태가 null인 경우 모든 상태로 조회해야 한다")
    fun `상태가 null인 경우 모든 상태로 조회해야 한다`() {
        // Given
        val filter = QueryFilter(
            partnerId = 1L,
            status = null,
            limit = 15
        )

        val mixedPayments = listOf(
            Payment(
                id = 1L,
                partnerId = 1L,
                amount = BigDecimal("10000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("235"),
                netAmount = BigDecimal("9765"),
                status = PaymentStatus.APPROVED
            ),
            Payment(
                id = 2L,
                partnerId = 1L,
                amount = BigDecimal("5000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("118"),
                netAmount = BigDecimal("4882"),
                status = PaymentStatus.CANCELED
            )
        )

        val pageResult = PaymentPage(
            items = mixedPayments,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryProjection = PaymentSummaryProjection(
            count = 2L,
            totalAmount = BigDecimal("15000"),
            totalNetAmount = BigDecimal("14647")
        )

        // Mock 설정
        every { cursorEncoder.decode(null) } returns null
        every { paymentRepository.findBy(any<PaymentQuery>()) } returns pageResult
        every { paymentRepository.summary(any<PaymentSummaryFilter>()) } returns summaryProjection

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(2L, result.summary.count)

        // 검증
        val querySlot = slot<PaymentQuery>()
        verify { paymentRepository.findBy(capture(querySlot)) }
        assertEquals(null, querySlot.captured.status)

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        verify { paymentRepository.summary(capture(summaryFilterSlot)) }
        assertEquals(null, summaryFilterSlot.captured.status)
    }

    @Test
    @DisplayName("잘못된 상태 문자열인 경우 null로 처리해야 한다")
    fun `잘못된 상태 문자열인 경우 null로 처리해야 한다`() {
        // Given
        val filter = QueryFilter(
            partnerId = 1L,
            status = "INVALID_STATUS",
            limit = 10
        )

        val emptyPayments = emptyList<Payment>()
        val pageResult = PaymentPage(
            items = emptyPayments,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryProjection = PaymentSummaryProjection(
            count = 0L,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO
        )

        // Mock 설정
        every { cursorEncoder.decode(null) } returns null
        every { paymentRepository.findBy(any<PaymentQuery>()) } returns pageResult
        every { paymentRepository.summary(any<PaymentSummaryFilter>()) } returns summaryProjection

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertNotNull(result)
        assertEquals(0, result.items.size)

        // 검증 - 잘못된 상태는 null로 변환되어야 함
        val querySlot = slot<PaymentQuery>()
        verify { paymentRepository.findBy(capture(querySlot)) }
        assertEquals(null, querySlot.captured.status)

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        verify { paymentRepository.summary(capture(summaryFilterSlot)) }
        assertEquals(null, summaryFilterSlot.captured.status)
    }

    @Test
    @DisplayName("더 이상 페이지가 없는 경우 nextCursor가 null이어야 한다")
    fun `더 이상 페이지가 없는 경우 nextCursor가 null이어야 한다`() {
        // Given
        val filter = QueryFilter(
            partnerId = 1L,
            status = "APPROVED",
            limit = 10
        )

        val payments = listOf(
            Payment(
                id = 1L,
                partnerId = 1L,
                amount = BigDecimal("10000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("235"),
                netAmount = BigDecimal("9765"),
                status = PaymentStatus.APPROVED
            )
        )

        val pageResult = PaymentPage(
            items = payments,
            hasNext = false, // 마지막 페이지
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryProjection = PaymentSummaryProjection(
            count = 1L,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9765")
        )

        // Mock 설정
        every { cursorEncoder.decode(null) } returns null
        every { paymentRepository.findBy(any<PaymentQuery>()) } returns pageResult
        every { paymentRepository.summary(any<PaymentSummaryFilter>()) } returns summaryProjection

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertNull(result.nextCursor)
        assertEquals(false, result.hasNext)

        // cursorEncoder.encode가 호출되지 않아야 함
        verify(exactly = 0) { cursorEncoder.encode(any<Instant>(), any<Long>()) }
    }










    @Test
    @DisplayName("파트너 ID가 null인 경우 전체 파트너 대상으로 조회해야 한다")
    fun `파트너 ID가 null인 경우 전체 파트너 대상으로 조회해야 한다`() {
        // Given
        val filter = QueryFilter(
            partnerId = null,
            status = "APPROVED",
            limit = 5
        )

        val crossPartnerPayments = listOf(
            Payment(
                id = 1L,
                partnerId = 1L,
                amount = BigDecimal("10000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("235"),
                netAmount = BigDecimal("9765"),
                status = PaymentStatus.APPROVED
            ),
            Payment(
                id = 2L,
                partnerId = 2L,
                amount = BigDecimal("15000"),
                appliedFeeRate = BigDecimal("0.0235"),
                feeAmount = BigDecimal("353"),
                netAmount = BigDecimal("14647"),
                status = PaymentStatus.APPROVED
            )
        )

        val pageResult = PaymentPage(
            items = crossPartnerPayments,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryProjection = PaymentSummaryProjection(
            count = 2L,
            totalAmount = BigDecimal("25000"),
            totalNetAmount = BigDecimal("24412")
        )

        // Mock 설정
        every { cursorEncoder.decode(null) } returns null
        every { paymentRepository.findBy(any<PaymentQuery>()) } returns pageResult
        every { paymentRepository.summary(any<PaymentSummaryFilter>()) } returns summaryProjection

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(2L, result.summary.count)

        // 검증 - 파트너 ID가 null로 전달되어야 함
        val querySlot = slot<PaymentQuery>()
        verify { paymentRepository.findBy(capture(querySlot)) }
        assertEquals(null, querySlot.captured.partnerId)

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        verify { paymentRepository.summary(capture(summaryFilterSlot)) }
        assertEquals(null, summaryFilterSlot.captured.partnerId)
    }

}
