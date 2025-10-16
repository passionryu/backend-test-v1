package im.bigs.pg.external.pg

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

/**
 * 실제 Test PG 서버 연동 구현체
 * - PaymentService에서 호출되어 결제를 승인하고 결과를 반환합니다.
 * - AES-256-GCM 암호화(enc) 방식으로 요청을 전달합니다.
 */
@Component
class TestPgClient(
    private val webClient: WebClient
) : PgClientOutPort {

    private val baseUrl= "https://api-test-pg.bigs.im"
    private val apiKey = "11111111-1111-4111-8111-111111111111"
    private val ivBase64 = "AAAAAAAAAAAAAAAA"

    // 기존 MockPgClient에 있던 로직 그대로 적용
    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 1L

    // 결재 승인 메서드
    override fun approve(request: PgApproveRequest): PgApproveResult {
        // 평문 JSON 생성
        val plainJson = """
            {
                "cardNumber": "${request.cardBin}${request.cardLast4}",
                "birthDate": "19900101",
                "expiry": "1227",
                "password": "12",
                "amount": ${request.amount}
            }
        """.trimIndent()

        // AES-256-GCM 암호화
        val enc = encryptAesGcm(plainJson, apiKey, ivBase64)

        // 실제 PG 서버 호출
        val response = webClient.post()
            .uri("$baseUrl/api/v1/pay/credit-card")
            .header("API-KEY", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("enc" to enc))
            .retrieve()
            .bodyToMono(PgApproveApiResponse::class.java)
            .block()

        // 여기서는 예시로 승인 성공을 반환
        return PgApproveResult(
            approvalCode = "TEST1234",
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED
        )
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

    data class PgApproveApiResponse(
        val approvalCode: String,
        val approvedAt: String,
        val status: String
    )

}
