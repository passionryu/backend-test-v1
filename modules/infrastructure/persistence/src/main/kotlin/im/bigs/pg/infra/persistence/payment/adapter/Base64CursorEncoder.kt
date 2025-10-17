package im.bigs.pg.infra.persistence.payment.adapter

import im.bigs.pg.application.payment.service.CursorEncoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

@Component
class Base64CursorEncoder : CursorEncoder {

    /** 다음 페이지 이동을 위한 커서 인코딩. */
    override fun encode(createdAt: Instant, id: Long): String {
        val raw = "${createdAt.toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /** 요청으로 전달된 커서 복원. 유효하지 않으면 null 커서로 간주합니다. */
    override fun decode(cursor: String?): Pair<LocalDateTime?, Long?>? {
        if (cursor.isNullOrBlank()) return null
        return try {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val (ts, idStr) = raw.split(":")
            val instant = Instant.ofEpochMilli(ts.toLong())
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC) to idStr.toLong()
        } catch (e: Exception) {
            null
        }
    }
}