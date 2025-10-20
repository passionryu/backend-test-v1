package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.manager.FeePolicyManager
import im.bigs.pg.application.partner.manager.PartnerManager
import im.bigs.pg.application.payment.manager.PaymentAuthorizationManager
import im.bigs.pg.application.payment.manager.PaymentBuilder
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.manager.PgClientManager
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentServiceTest {

    private val partnerManager = mockk<PartnerManager>()
    private val pgClientManager = mockk<PgClientManager>()
    private val paymentAuthorizationManager = mockk<PaymentAuthorizationManager>()
    private val feePolicyManager = mockk<FeePolicyManager>()
    private val paymentBuilder = mockk<PaymentBuilder>()
    private val paymentRepository = mockk<PaymentOutPort>()

    private val paymentService = PaymentService(
        partnerManager = partnerManager,
        pgClientManager = pgClientManager,
        paymentAuthorizationManager = paymentAuthorizationManager,
        feePolicyManager = feePolicyManager,
        paymentBuilder = paymentBuilder,
        paymentRepository = paymentRepository
    )

    @Test
    @DisplayName("결제 성공 시 모든 단계가 올바르게 실행되어야 한다")
    fun `결제 성공 시 모든 단계가 올바르게 실행되어야 한다`() = runBlocking {
        // Given
        val partnerId = 1L
        val amount = BigDecimal("10000")
        val command = PaymentCommand(
            partnerId = partnerId,
            amount = amount,
            cardBin = "123456",
            cardLast4 = "7890",
            birthDate = "19900101",
            expiry = "1227",
            password = "12",
            productName = "테스트 상품"
        )

        val partner = Partner(
            id = partnerId,
            code = "TEST_PARTNER",
            name = "테스트 파트너",
            active = true
        )

        val pgClient = mockk<PgClientOutPort>()

        val approveResult = PgApproveResult(
            approvalCode = "APPROVAL-123",
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED
        )

        val feePolicy = FeePolicy(
            id = 1L,
            partnerId = partnerId,
            effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0235"), // 2.35%
            fixedFee = BigDecimal("100")
        )

        // 실제 FeeCalculator를 사용하여 계산값 확인
        val (calculatedFee, calculatedNet) = FeeCalculator.calculateFee(amount, feePolicy.percentage, feePolicy.fixedFee)
        // 10000 * 0.0235 + 100 = 235 + 100 = 335
        // 10000 - 335 = 9665

        val expectedPayment = Payment(
            id = 1L,
            partnerId = partnerId,
            amount = amount,
            appliedFeeRate = feePolicy.percentage,
            feeAmount = calculatedFee,
            netAmount = calculatedNet,
            cardBin = command.cardBin,
            cardLast4 = command.cardLast4,
            approvalCode = approveResult.approvalCode,
            approvedAt = approveResult.approvedAt,
            status = PaymentStatus.APPROVED
        )

        val savedPayment = expectedPayment.copy(id = 99L)

        // When & Then
        every { partnerManager.findPartner(partnerId) } returns partner
        every { pgClientManager.findPgClient(partnerId) } returns pgClient
        coEvery {
            paymentAuthorizationManager.authorizePayment(pgClient, partnerId, command)
        } returns approveResult
        every { feePolicyManager.findFeePolicy(partnerId) } returns feePolicy
        every {
            paymentBuilder.buildPayment(partnerId, command, approveResult, feePolicy, calculatedFee, calculatedNet)
        } returns expectedPayment
        every { paymentRepository.save(expectedPayment) } returns savedPayment

        val result = paymentService.pay(command)

        // Then
        assertEquals(savedPayment.id, result.id)
        assertEquals(amount, result.amount)
        assertEquals(calculatedFee, result.feeAmount)
        assertEquals(calculatedNet, result.netAmount)
        assertEquals(PaymentStatus.APPROVED, result.status)
        assertEquals(approveResult.approvalCode, result.approvalCode)

        verify { partnerManager.findPartner(partnerId) }
        verify { pgClientManager.findPgClient(partnerId) }
        coVerify { paymentAuthorizationManager.authorizePayment(pgClient, partnerId, command) }
        verify { feePolicyManager.findFeePolicy(partnerId) }
        verify { paymentRepository.save(expectedPayment) }
    }

    @Test
    @DisplayName("결제 실패 시 취소된 결제가 저장되어야 한다")
    fun `결제 실패 시 취소된 결제가 저장되어야 한다`() = runBlocking {
        // Given
        val partnerId = 1L
        val amount = BigDecimal("5000")
        val command = PaymentCommand(
            partnerId = partnerId,
            amount = amount,
            cardBin = "111111",
            cardLast4 = "2222",
            birthDate = "19950101",
            expiry = "1228",
            password = "34",
            productName = "실패 상품"
        )

        val partner = Partner(
            id = partnerId,
            code = "FAIL_PARTNER",
            name = "실패 파트너",
            active = true
        )

        val pgClient = mockk<PgClientOutPort>()

        val failedApproveResult = PgApproveResult(
            approvalCode = null,
            approvedAt = null,
            status = PaymentStatus.CANCELED,
            failureReason = "잔액 부족"
        )

        val feePolicy = FeePolicy(
            id = 2L,
            partnerId = partnerId,
            effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"), // 3.0%
            fixedFee = null // 정액 수수료 없음
        )

        // 실제 FeeCalculator를 사용하여 계산값 확인
        val (calculatedFee, calculatedNet) = FeeCalculator.calculateFee(amount, feePolicy.percentage, feePolicy.fixedFee)
        // 5000 * 0.0300 = 150
        // 5000 - 150 = 4850

        val expectedPayment = Payment(
            id = null,
            partnerId = partnerId,
            amount = amount,
            appliedFeeRate = feePolicy.percentage,
            feeAmount = calculatedFee,
            netAmount = calculatedNet,
            cardBin = command.cardBin,
            cardLast4 = command.cardLast4,
            status = PaymentStatus.CANCELED,
            canceledReason = failedApproveResult.failureReason,
            failedAt = LocalDateTime.now(ZoneOffset.UTC)
        )

        val savedPayment = expectedPayment.copy(id = 100L)

        // When & Then
        every { partnerManager.findPartner(partnerId) } returns partner
        every { pgClientManager.findPgClient(partnerId) } returns pgClient
        coEvery {
            paymentAuthorizationManager.authorizePayment(pgClient, partnerId, command)
        } returns failedApproveResult
        every { feePolicyManager.findFeePolicy(partnerId) } returns feePolicy
        every {
            paymentBuilder.buildPayment(partnerId, command, failedApproveResult, feePolicy, calculatedFee, calculatedNet)
        } returns expectedPayment
        every { paymentRepository.save(expectedPayment) } returns savedPayment

        val result = paymentService.pay(command)

        // Then
        assertEquals(savedPayment.id, result.id)
        assertEquals(amount, result.amount)
        assertEquals(calculatedFee, result.feeAmount)
        assertEquals(calculatedNet, result.netAmount)
        assertEquals(PaymentStatus.CANCELED, result.status)
        assertEquals(failedApproveResult.failureReason, result.canceledReason)
        assertEquals(null, result.approvalCode)

        verify { partnerManager.findPartner(partnerId) }
        verify { pgClientManager.findPgClient(partnerId) }
        coVerify { paymentAuthorizationManager.authorizePayment(pgClient, partnerId, command) }
        verify { feePolicyManager.findFeePolicy(partnerId) }
        verify { paymentRepository.save(expectedPayment) }
    }

    @Test
    @DisplayName("잘못된 파트너 ID로 결제 요청 시 예외가 발생해야 한다")
    fun `잘못된 파트너 ID로 결제 요청 시 예외가 발생해야 한다`() = runBlocking {
        // Given
        val invalidPartnerId = 999L
        val command = PaymentCommand(
            partnerId = invalidPartnerId,
            amount = BigDecimal("1000"),
            cardBin = "123456",
            cardLast4 = "7890",
            birthDate = "19900101",
            expiry = "1227",
            password = "12",
            productName = "테스트 상품"
        )

        // When & Then
        every { partnerManager.findPartner(invalidPartnerId) } throws IllegalArgumentException("Partner not found: $invalidPartnerId")

        assertFailsWith<IllegalArgumentException> {
            paymentService.pay(command)
        }

        verify { partnerManager.findPartner(invalidPartnerId) }
        verify(exactly = 0) { pgClientManager.findPgClient(any()) }
        coVerify(exactly = 0) { paymentAuthorizationManager.authorizePayment(any(), any(), any()) }
        verify(exactly = 0) { feePolicyManager.findFeePolicy(any()) }
        verify(exactly = 0) { paymentRepository.save(any()) }
    }

    @Test
    @DisplayName("비활성화된 파트너로 결제 요청 시 예외가 발생해야 한다")
    fun `비활성화된 파트너로 결제 요청 시 예외가 발생해야 한다`() = runBlocking {
        // Given
        val inactivePartnerId = 2L
        val command = PaymentCommand(
            partnerId = inactivePartnerId,
            amount = BigDecimal("1000"),
            cardBin = "123456",
            cardLast4 = "7890",
            birthDate = "19900101",
            expiry = "1227",
            password = "12",
            productName = "테스트 상품"
        )

        // When & Then
        every { partnerManager.findPartner(inactivePartnerId) } throws IllegalStateException("Partner is inactive: $inactivePartnerId")

        assertFailsWith<IllegalStateException> {
            paymentService.pay(command)
        }

        verify { partnerManager.findPartner(inactivePartnerId) }
        verify(exactly = 0) { pgClientManager.findPgClient(any()) }
        coVerify(exactly = 0) { paymentAuthorizationManager.authorizePayment(any(), any(), any()) }
        verify(exactly = 0) { feePolicyManager.findFeePolicy(any()) }
        verify(exactly = 0) { paymentRepository.save(any()) }
    }

}
