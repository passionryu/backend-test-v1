package im.bigs.pg.application.pg.manager

import im.bigs.pg.common.PgClientOutPort
import org.springframework.stereotype.Component

/**
 * Partner와 연결된 PG(Client) 조회를 담당합니다.
 * - 여러 PG가 존재할 수 있으므로, partnerId에 맞는 PG를 찾아 반환
 */
@Component
class PgClientManager(
    private val pgClients: List<PgClientOutPort>
) {

    /**
     * 특정 Partner를 지원하는 PG Client를 조회합니다.
     *
     * @param partnerId 조회할 Partner ID
     * @return 해당 Partner를 지원하는 PG Client
     * @throws IllegalStateException 지원하는 PG Client가 없을 경우
     */
    fun findPgClient(partnerId: Long): PgClientOutPort {
        return pgClients.firstOrNull { it.supports(partnerId) }
            ?: throw IllegalStateException("No PG client supports partner: $partnerId")
    }
}
