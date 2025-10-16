package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class 결제서비스Test {
    private val partnerRepo = mockk<PartnerOutPort>()
    private val feeRepo = mockk<FeePolicyOutPort>()
    private val paymentRepo = mockk<PaymentOutPort>()
    private val pgClient = object : PgClientOutPort {
        override fun supports(partnerId: Long) = true
        override suspend fun approve(request: PgApproveRequest) =
            PgApproveResult("APPROVAL-123", LocalDateTime.of(2024, 1, 1, 0, 0), PaymentStatus.APPROVED)
    }

    @Test
    @DisplayName("결제 시 수수료 정책을 적용하고 저장해야 한다")
    fun `결제 시 수수료 정책을 적용하고 저장해야 한다`() = runBlocking {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L, partnerId = 1L,
            effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"), fixedFee = BigDecimal("100")
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardBin = "1111" ,cardLast4 = "4242", birthDate = "19900101" , expiry = "1227", password = "12")
        val res = service.pay(cmd) // suspend 함수 호출 가능

        assertEquals(99L, res.id)
        assertEquals(BigDecimal("400"), res.feeAmount)
        assertEquals(BigDecimal("9600"), res.netAmount)
        assertEquals(PaymentStatus.APPROVED, res.status)
    }
//
//    @Test
//    @DisplayName("결제 시 수수료 정책을 적용하고 저장해야 한다")
//    fun `결제 시 수수료 정책을 적용하고 저장해야 한다`() {
//        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))
//        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
//        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
//            id = 10L, partnerId = 1L, effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
//            percentage = BigDecimal("0.0300"), fixedFee = BigDecimal("100")
//        )
//        val savedSlot = slot<Payment>()
//        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }
//
//        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")
//        val res = service.pay(cmd)
//
//        assertEquals(99L, res.id)
//        assertEquals(BigDecimal("400"), res.feeAmount)
//        assertEquals(BigDecimal("9600"), res.netAmount)
//        assertEquals(PaymentStatus.APPROVED, res.status)
//    }
}
