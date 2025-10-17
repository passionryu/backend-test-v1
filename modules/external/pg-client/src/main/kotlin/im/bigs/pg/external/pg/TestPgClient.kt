package im.bigs.pg.external.pg

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.PgApproveResult
import im.bigs.pg.common.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.config.PgClientProperties
import im.bigs.pg.external.pg.dto.PgApproveApiResponse
import im.bigs.pg.external.pg.helper.buildEncryptedRequestBody
import im.bigs.pg.external.pg.helper.toApproveResult
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

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

    /**
     * 결재 승인 메서드
     *
     * 성공 시 : PG 서버 응답을 도메인용 PgApproveResult로 변환
     * 실패 시 : 예외 발생 -> 결제 실패(PaymentStatus.CANCELED)로 처리 -> 실패 사유&실패 일시 기록/반환
     */
    override suspend fun approve(request: PgApproveRequest): PgApproveResult {

        return try {
            val requestBody = buildEncryptedRequestBody(request, apiKey, ivBase64) // 요청 암호화
            val response = webClient.post()
                .uri("$baseUrl/api/v1/pay/credit-card")
                .header("API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(PgApproveApiResponse::class.java)
                .timeout(Duration.ofSeconds(10)) // TimeOut : 10초 이상 응답이 없으면 TimeoutException 발생
                .awaitSingle() // Non-Blocking : suspend/Coroutine 환경

            toApproveResult(response) // 결제 승인 결과
        } catch (e: Exception) {
            return PgApproveResult(
                approvalCode = null,
                approvedAt = null,
                status = PaymentStatus.CANCELED,
                failureReason = e.message ?: "Unknown error"
            )
        }
    }

}
