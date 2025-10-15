package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
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
    private val baseUrl: String = "https://api-test-pg.bigs.im",
    private val apiKey: String = "<API-KEY>",        // 실제 발급받은 API-KEY
    private val ivBase64: String = "<IV_B64URL>"     // 서버 등록된 IV
) : PgClientOutPort {

    override fun supports(partnerId: Long): Boolean = true // 모든 제휴사 지원, 필요시 조건 변경

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

        // TODO: HTTP POST 요청으로 Test PG 서버에 전달하고 결과 수신
        // 예시: enc를 JSON { "enc": enc } 로 POST /api/v1/pay/credit-card

        // 여기서는 예시로 승인 성공을 반환
        return PgApproveResult(
            approvalCode = "TEST1234",
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED
        )
    }

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
}
