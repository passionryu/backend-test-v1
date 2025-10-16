package im.bigs.pg.external.pg.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "pg.client")
class PgClientProperties {
    lateinit var apiKey: String
    lateinit var ivBase64: String
}
