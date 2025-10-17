package im.bigs.pg.application.payment.helper

import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary

object PaymentSummaryHelper {

    fun fetchSummary(
        repository: PaymentOutPort,
        partnerId: Long?,
        status: PaymentStatus?,
        from: java.time.LocalDateTime?,
        to: java.time.LocalDateTime?
    ): PaymentSummary {
        val filter = PaymentSummaryFilter(
            partnerId = partnerId,
            status = status,
            from = from,
            to = to
        )

        val projection = repository.summary(filter)
        return PaymentSummary(
            count = projection.count,
            totalAmount = projection.totalAmount,
            totalNetAmount = projection.totalNetAmount
        )
    }
}
