package im.bigs.pg.infra.persistence.payment.repository

import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

/** 결제 이력 조회용 JPA 리포지토리. */
interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long>,
    JpaSpecificationExecutor<PaymentEntity>,
    PaymentCustomRepository {

    /** 통계 합계/건수 조회. */
    @Query(
        value = """
    select count(p.id) as cnt, coalesce(sum(p.amount),0) as totalAmount, coalesce(sum(p.net_amount),0) as totalNet
    from payment p
    where (coalesce(:partnerId, -1) = -1 or p.partner_id = :partnerId)
      and (coalesce(:status, '') = '' or p.status = :status)
      and (coalesce(:fromAt, '1900-01-01'::timestamp) = '1900-01-01'::timestamp or p.created_at >= :fromAt)
      and (coalesce(:toAt, '2100-01-01'::timestamp) = '2100-01-01'::timestamp or p.created_at < :toAt)
    """,
        nativeQuery = true
    )
    fun summary(
        @Param("partnerId") partnerId: Long?,
        @Param("status") status: String?,
        @Param("fromAt") fromAt: Instant?,
        @Param("toAt") toAt: Instant?,
    ): List<Array<Any>>

}
