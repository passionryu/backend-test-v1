package im.bigs.pg.infra.persistence.cache

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * HTTP 요청 단위로 캐시 로그를 묶어서 출력하는 필터.
 */
@Component
class CacheLoggingFilter : Filter {
    private val logger = LoggerFactory.getLogger(CacheLoggingFilter::class.java)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as? HttpServletRequest
        val uri = httpRequest?.requestURI ?: "unknown"
        val startTime = System.currentTimeMillis()

        // 요청 시작 시 버퍼 초기화
        LoggingCache.requestLogs.set(mutableListOf())

        try {
            chain.doFilter(request, response)
        } finally {
            val logs = LoggingCache.requestLogs.get()
            if (logs.isNotEmpty()) {
                val duration = System.currentTimeMillis() - startTime
                logger.info("|--------------------------------------------------------------|")
                logs.forEach { logger.info(it) }
                logger.info("|--------------------------------------------------------------| [${uri}] (${duration}ms)")
            }
            LoggingCache.requestLogs.remove()
        }
    }
}
