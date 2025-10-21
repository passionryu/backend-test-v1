package im.bigs.pg.application.payment.manager

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class EnhancedPaymentCacheManager(private val cacheManager: CacheManager) {
    /**
     * 특정 제휴사의 결제 조회 캐시를 무효화합니다.
     * 새 결제가 생성되면 해당 제휴사의 모든 조회 결과 캐시를 삭제하여
     * 데이터 일관성을 보장합니다.
     */
    public fun evictPartnerCache(partnerId: Long) {
        try {
            // 결제 조회 캐시 무효화
            val queryCache = cacheManager.getCache("paymentQueries")
            queryCache?.clear()

            // 결제 통계 캐시 무효화
            val summaryCache = cacheManager.getCache("paymentSummaries")
            summaryCache?.clear()
        } catch (e: Exception) {
            // 캐시 무효화 실패는 비즈니스 로직에 영향을 주지 않도록 로그만 남김
            // 실제 운영에서는 로깅 프레임워크 사용 권장
            println("캐시 무효화 실패 (제휴사 ID: $partnerId): ${e.message}")
        }
    }
}