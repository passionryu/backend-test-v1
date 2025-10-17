package im.bigs.pg.application.payment.helper

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

object PaymentQueryHelper {
    fun fetchPayments(
        repository: PaymentOutPort,
        filter: QueryFilter,
        paymentStatus: PaymentStatus?,
        cursorInfo: Pair<LocalDateTime?, Long?>?
    ) = repository.findBy(
        PaymentQuery(
            partnerId = filter.partnerId,
            status = paymentStatus,
            from = filter.from,
            to = filter.to,
            limit = filter.limit,
            cursorCreatedAt = cursorInfo?.first,
            cursorId = cursorInfo?.second
        )
    )
}
