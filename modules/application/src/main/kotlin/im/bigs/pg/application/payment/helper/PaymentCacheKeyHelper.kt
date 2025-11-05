package im.bigs.pg.application.payment.helper

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.domain.payment.PaymentStatus
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.JvmStatic

/**
 * 결제 캐시 키 생성 헬퍼
 * - 캐시 키 일관성을 위해 중앙화된 키 생성 로직 제공
 */
object PaymentCacheKeyHelper {

    private val keyCache = ConcurrentHashMap<String, String>()

    /**
     * 결제 조회 결과 캐시 키 생성
     * @param filter 조회 필터 조건
     * @param paymentStatus 결제 상태
     * @param cursorInfo 커서 정보 (첫 번째 페이지의 경우 null)
     */
    @JvmStatic
    fun generateQueryCacheKey(
        filter: QueryFilter,
        paymentStatus: PaymentStatus?,
        cursorInfo: Pair<java.time.LocalDateTime?, Long?>?
    ): String {
        val keyBase = buildString {
            append("payment:query")
            append(":partnerId=").append(filter.partnerId ?: "null")
            append(":status=").append(paymentStatus?.name ?: "null")
            append(":from=").append(filter.from?.toString() ?: "null")
            append(":to=").append(filter.to?.toString() ?: "null")
            append(":limit=").append(filter.limit)
            
            // 커서 정보가 있으면 커서 기반 키 사용 (페이지네이션)
            if (cursorInfo != null) {
                append(":cursorAt=").append(cursorInfo.first?.toString() ?: "null")
                append(":cursorId=").append(cursorInfo.second ?: "null")
            } else {
                append(":page=first")
            }
        }
        
        return keyCache.computeIfAbsent(keyBase) { keyBase }
    }

    /**
     * 결제 통계 캐시 키 생성
     * @param partnerId 제휴사 ID
     * @param paymentStatus 결제 상태
     * @param from 조회 시작 시각
     * @param to 조회 종료 시각
     */
    @JvmStatic
    fun generateSummaryCacheKey(
        partnerId: Long?,
        paymentStatus: PaymentStatus?,
        from: java.time.LocalDateTime?,
        to: java.time.LocalDateTime?
    ): String {
        val keyBase = buildString {
            append("payment:summary")
            append(":partnerId=").append(partnerId ?: "null")
            append(":status=").append(paymentStatus?.name ?: "null")
            append(":from=").append(from?.toString() ?: "null")
            append(":to=").append(to?.toString() ?: "null")
        }
        
        return keyCache.computeIfAbsent(keyBase) { keyBase }
    }

    /**
     * 특정 제휴사의 모든 결제 조회 캐시 키 패턴 생성 (Eviction용)
     * @param partnerId 제휴사 ID
     */
    fun generatePartnerEvictionPattern(partnerId: Long): String {
        return "payment:query:partnerId=${partnerId}:*"
    }

    /**
     * 특정 제휴사의 모든 통계 캐시 키 패턴 생성 (Eviction용)
     * @param partnerId 제휴사 ID
     */
    fun generatePartnerSummaryEvictionPattern(partnerId: Long): String {
        return "payment:summary:partnerId=${partnerId}:*"
    }
}



