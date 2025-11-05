package im.bigs.pg.api.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
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
    private fun jacksonObjectMapperForRedis(): ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    private fun <T> jacksonValueSerializer(targetClass: Class<T>): Jackson2JsonRedisSerializer<T> {
        val serializer = Jackson2JsonRedisSerializer(targetClass)
        serializer.setObjectMapper(jacksonObjectMapperForRedis())
        return serializer
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        logger.info("[CACHE] Redis 템플릿 초기화 시작")
        
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        // 키 직렬화 설정
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        
        // 값 직렬화 설정 (JSON 형태)
        // 템플릿은 범용 Generic 사용 가능하지만, 캐시 경로는 per-cache로 구체 타입 사용
        val generic = GenericJackson2JsonRedisSerializer(jacksonObjectMapperForRedis())
        template.valueSerializer = generic
        template.hashValueSerializer = generic
        
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
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(GenericJackson2JsonRedisSerializer(jacksonObjectMapperForRedis()))
            )

        // 캐시별 구체 타입 직렬화기 지정 (다형성/WRAPPER 포맷 충돌 제거)
        val paymentQueriesConfig = defaultConfig.serializeValuesWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(jacksonValueSerializer(im.bigs.pg.application.payment.port.out.PaymentPage::class.java))
        )
        val paymentSummariesConfig = defaultConfig.serializeValuesWith(
            RedisSerializationContext.SerializationPair
                .fromSerializer(jacksonValueSerializer(im.bigs.pg.domain.payment.PaymentSummary::class.java))
        )

        val cacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            // 결제 조회 캐시 설정 (10분 TTL) + PaymentPage 타입 직렬화기
            .withCacheConfiguration(
                "paymentQueries",
                paymentQueriesConfig.entryTtl(java.time.Duration.ofMinutes(10))
            )
            // 결제 통계 캐시 설정 (15분 TTL) + PaymentSummary 타입 직렬화기
            .withCacheConfiguration(
                "paymentSummaries",
                paymentSummariesConfig.entryTtl(java.time.Duration.ofMinutes(15))
            )
            .build()
        return cacheManager
    }
}
