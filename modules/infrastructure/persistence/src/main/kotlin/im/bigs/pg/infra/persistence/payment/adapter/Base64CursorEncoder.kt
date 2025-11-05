package im.bigs.pg.infra.persistence.payment.adapter

import im.bigs.pg.application.payment.service.CursorEncoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

/**
 * Base64 기반 커서 인코더 구현체.
 *
 * createdAt(시간)과 id를 Base64 문자열로 인코딩/디코딩하여
 * 커서 기반 페이지네이션(cursor pagination)에 사용된다.
 *
 * 예: "1730812345678:42" → "MTczMDgxMjM0NTY3ODo0Mg"
 */
@Component
class Base64CursorEncoder : CursorEncoder {

    /**
     * createdAt과 id를 결합하여 Base64 문자열로 인코딩한다.
     *
     * @param createdAt 커서 기준 시간 (결제 생성 시각 등)
     * @param id 커서 기준 식별자
     * @return Base64 URL-safe 형태로 인코딩된 커서 문자열
     */
    override fun encode(createdAt: Instant, id: Long): String {
        val raw = "${createdAt.toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /**
     * 인코딩된 Base64 커서 문자열을 복호화하여 (createdAt, id)로 복원한다.
     *
     * @param cursor Base64로 인코딩된 커서 문자열
     * @return (생성일시, 식별자) 쌍 또는 null
     */
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