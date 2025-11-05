package im.bigs.pg.infra.persistence.cache

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class LoggingCacheManager(private val delegate: CacheManager) : CacheManager {
    override fun getCache(name: String): Cache? {
        val cache = delegate.getCache(name)
        return if (cache != null) LoggingCache(cache) else null
    }

    override fun getCacheNames(): Collection<String> = delegate.cacheNames
}
