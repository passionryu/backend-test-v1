package im.bigs.pg.application.payment.manager

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class EnhancedPaymentCacheManager(private val cacheManager: CacheManager) {

    private val logger = org.slf4j.LoggerFactory.getLogger(EnhancedPaymentCacheManager::class.java)

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
            logger.warn("캐시 무효화 실패 (제휴사 ID: $partnerId): ${e.message}")
        }
    }
}