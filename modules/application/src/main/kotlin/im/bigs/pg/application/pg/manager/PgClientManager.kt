package im.bigs.pg.application.pg.manager

import im.bigs.pg.common.PgClientOutPort
import org.springframework.stereotype.Component

@Component
class PgClientManager(
    private val pgClients: List<PgClientOutPort>
) {
    fun findPgClient(partnerId: Long): PgClientOutPort {
        return pgClients.firstOrNull { it.supports(partnerId) }
            ?: throw IllegalStateException("No PG client supports partner: $partnerId")
    }
}
