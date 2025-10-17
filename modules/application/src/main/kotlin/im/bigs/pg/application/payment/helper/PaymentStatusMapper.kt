package im.bigs.pg.application.payment.helper

import im.bigs.pg.domain.payment.PaymentStatus

object PaymentStatusMapper {
    fun from(value: String?): PaymentStatus? {
        return try {
            value?.let { PaymentStatus.valueOf(it.uppercase()) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}