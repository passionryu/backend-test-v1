package im.bigs.pg.infra.persistence.cache

import org.springframework.cache.Cache
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

/**
 * 각 요청에서 발생한 캐시 HIT/MISS 로그를 ThreadLocal에 버퍼링하는 캐시 래퍼.
 */
class LoggingCache(private val delegate: Cache) : Cache {
    private val logger = LoggerFactory.getLogger(LoggingCache::class.java)

    companion object {
        /** 요청 단위 캐시 로그를 모으는 ThreadLocal 버퍼 */
        val requestLogs: ThreadLocal<MutableList<String>> = ThreadLocal.withInitial { mutableListOf() }
    }

    override fun getName(): String = delegate.name
    override fun getNativeCache(): Any = delegate.nativeCache

    private fun record(message: String) {
        requestLogs.get().add(message)
    }

    override fun get(key: Any): Cache.ValueWrapper? {
        val start = System.currentTimeMillis()
        val value = delegate.get(key)
        val duration = System.currentTimeMillis() - start
        val status = if (value != null) "HIT" else "MISS"
        record("[CACHE $status] name=${delegate.name}, 걸린 시간=${duration}ms")
        return value
    }

    override fun <T : Any> get(key: Any, type: Class<T>?): T? {
        val start = System.currentTimeMillis()
        val value = delegate.get(key, type)
        val duration = System.currentTimeMillis() - start
        val status = if (value != null) "HIT" else "MISS"
        record("[CACHE $status] name=${delegate.name}, 걸린 시간=${duration}ms")
        return value
    }

    override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T? {
        val start = System.currentTimeMillis()
        val value = delegate.get(key, valueLoader)
        val duration = System.currentTimeMillis() - start
        val status = if (value != null) "LOAD/HIT" else "LOAD/MISS"
        record("[CACHE $status] name=${delegate.name}, 걸린 시간=${duration}ms")
        return value
    }

    override fun put(key: Any, value: Any?) {
        delegate.put(key, value)
        record("[CACHE PUT] name=${delegate.name}, valueSaved=${value != null}")
    }

    override fun evict(key: Any) {
        delegate.evict(key)
        record("[CACHE EVICT] name=${delegate.name}")
    }

    override fun clear() {
        delegate.clear()
        record("[CACHE CLEAR] name=${delegate.name}")
    }
}
