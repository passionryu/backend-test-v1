package im.bigs.pg.application.payment.helper

import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.service.CursorEncoder
import java.time.ZoneOffset

object PaymentCursorHelper {

    /**
     * 다음 커서 생성
     */
    fun buildNextCursor(pageResult: PaymentPage, cursorEncoder: CursorEncoder): String? =
        if (pageResult.hasNext && pageResult.nextCursorCreatedAt != null && pageResult.nextCursorId != null)
            cursorEncoder.encode(pageResult.nextCursorCreatedAt.toInstant(ZoneOffset.UTC), pageResult.nextCursorId)
        else null
}
