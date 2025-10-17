package im.bigs.pg.application.payment.service

import java.time.Instant
import java.time.LocalDateTime

interface CursorEncoder {
    fun encode(createdAt: Instant, id: Long): String
    fun decode(cursor: String?): Pair<LocalDateTime?, Long?>?
}
