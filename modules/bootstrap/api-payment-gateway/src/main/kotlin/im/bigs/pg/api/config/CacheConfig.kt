package im.bigs.pg.api.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.slf4j.LoggerFactory

/**
 * Redis 캐시 설정 클래스
 * - 헥사고날 아키텍처의 인프라스트럭처 계층에 해당
 * - 캐시 매니저와 Redis 템플릿을 설정하여 애노테이션 기반 캐싱 활성화
 */
@Configuration
@EnableCaching
class CacheConfig {

    private val logger = LoggerFactory.getLogger(CacheConfig::class.java)

    /**
     * Redis 템플릿 설정
     * - 키 직렬화: StringRedisSerializer 사용
     * - 값 직렬화: GenericJackson2JsonRedisSerializer 사용 (JSON 형태로 저장)
     */
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        logger.info("[CACHE] Redis 템플릿 초기화 시작")
        
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        // 키 직렬화 설정
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        
        // 값 직렬화 설정 (JSON 형태)
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        
        template.afterPropertiesSet()
        
        logger.info("[CACHE] Redis 템플릿 초기화 완료")
        return template
    }

    /**
     * Redis 캐시 매니저 설정
     * - RedisCacheManager를 사용하여 캐시 키 만료 시간 및 네이밍 정책 설정
     */
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        logger.info("[CACHE] Redis 캐시 매니저 초기화 시작")

        val defaultConfig = org.springframework.data.redis.cache.RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(java.time.Duration.ofMinutes(10)) // 기본 TTL: 10분
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                .fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(GenericJackson2JsonRedisSerializer()))

        val cacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            // 결제 조회 캐시 설정 (10분 TTL)
            .withCacheConfiguration("paymentQueries", defaultConfig.entryTtl(java.time.Duration.ofMinutes(10)))
            // 결제 통계 캐시 설정 (15분 TTL - 조회보다 조금 더 길게)
            .withCacheConfiguration("paymentSummaries", defaultConfig.entryTtl(java.time.Duration.ofMinutes(15)))
            .build()
        return cacheManager
    }
}
