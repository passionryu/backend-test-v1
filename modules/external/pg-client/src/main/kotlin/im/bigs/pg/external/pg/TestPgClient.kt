package im.bigs.pg.external.pg

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.config.PgClientProperties
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 실제 Test PG 서버 연동 구현체
 * - PaymentService에서 호출되어 결제를 승인하고 결과를 반환합니다.
 * - AES-256-GCM 암호화(enc) 방식으로 요청을 전달합니다.
 */
@Component
class TestPgClient(
    private val webClient: WebClient,
    private val properties: PgClientProperties
) : PgClientOutPort {

    private val baseUrl= "https://api-test-pg.bigs.im"
    private val apiKey = properties.apiKey
    private val ivBase64 = properties.ivBase64
    /**
     * 해당 구현체 활성화
     * - Boolean = true : 활성화
     * - Boolean = false : 비활성화
     */
    override fun supports(partnerId: Long): Boolean = true

    // 결재 승인 메서드
    override suspend fun approve(request: PgApproveRequest): PgApproveResult {

        try {
            val plainJson = """
        {
            "cardNumber": "${request.cardBin}-${request.cardBin}-${request.cardBin}-${request.cardLast4}",
            "birthDate": "${request.birthDate}",
            "expiry": "${request.expiry}",
            "password": "${request.password}",
            "amount": ${request.amount}
        }
        """.trimIndent()

            val enc = encryptAesGcm(plainJson, apiKey, ivBase64)
            val requestBody = mapOf("enc" to enc) // map -> json으로 직렬화 작업

            // WebClient 요청 (논블로킹, 타임아웃 적용)
            val response = webClient.post()
                .uri("$baseUrl/api/v1/pay/credit-card")
                .header("API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                // 10초 이상 응답이 없으면 TimeoutException 발생
                .bodyToMono(PgApproveApiResponse::class.java)
                .timeout(Duration.ofSeconds(10))
                .awaitSingle() // suspend/Coroutine 환경에서 논블로킹 호출

            return if (response?.status == "APPROVED") {
                // 결제 승인 시나리오
                PgApproveResult(
                    approvalCode = response.approvalCode,
                    approvedAt = LocalDateTime.parse(response.approvedAt.substringBefore('.')),
                    status = PaymentStatus.APPROVED
                )
            } else {
                // 결제 미승인 시나리오
                PgApproveResult(
                    approvalCode = null,
                    approvedAt = null,
                    status = PaymentStatus.CANCELED,
                    failureReason = response?.status ?: "Unknown failure"
                )
            }
        } catch (e: Exception) {
            // 예외 발생 시 실패로 처리
            return PgApproveResult(
                approvalCode = null,
                approvedAt = null,
                status = PaymentStatus.CANCELED,
                failureReason = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * 평문 JSON → AES-256-GCM 암호문(Base64URL) 변환합니다.
     * * 추후, 다른 Util 도메인에 책임 넘기기
     */
    private fun encryptAesGcm(plainText: String, apiKey: String, ivB64: String): String {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(apiKey.toByteArray(Charsets.UTF_8))
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivBytes = Base64.getUrlDecoder().decode(ivB64)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText)
    }

    // 이게 여기있는게 맞나...??
    // pgserver에서 오는 응답을 역직렬화 하는 DTO
    data class PgApproveApiResponse(
        val approvalCode: String,
        val approvedAt: String,
        val maskedCardLast4: String?,
        val amount: Long?,
        val status: String
    )

}
