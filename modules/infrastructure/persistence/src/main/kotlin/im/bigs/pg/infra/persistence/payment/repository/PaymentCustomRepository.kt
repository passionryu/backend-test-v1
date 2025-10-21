package im.bigs.pg.infra.persistence.payment.repository

import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import org.springframework.data.domain.Pageable

import java.time.Instant

interface PaymentCustomRepository {
    fun findPayments(
        partnerId: Long?,
        status: String?,
        fromAt: Instant?,
        toAt: Instant?,
        cursorCreatedAt: Instant?,
        cursorId: Long?,
        pageable: Pageable
    ): List<PaymentEntity>
}
