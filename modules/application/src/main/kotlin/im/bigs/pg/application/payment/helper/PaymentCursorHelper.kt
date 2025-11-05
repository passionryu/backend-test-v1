package im.bigs.pg.application.payment.helper

import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.service.CursorEncoder
import java.time.ZoneOffset

object PaymentCursorHelper {

    /**
     * 다음 페이지 커서를 생성한다.
     *
     * @param pageResult 페이지 조회 결과
     * @param cursorEncoder 커서 인코딩 전략
     * @return 다음 페이지 요청 시 사용할 인코딩된 커서 문자열, 또는 `null`
     */
    fun buildNextCursor(pageResult: PaymentPage, cursorEncoder: CursorEncoder): String? =
        if (pageResult.hasNext && pageResult.nextCursorCreatedAt != null && pageResult.nextCursorId != null)
            cursorEncoder.encode(pageResult.nextCursorCreatedAt.toInstant(ZoneOffset.UTC), pageResult.nextCursorId)
        else null
}
