package im.bigs.pg.infra.persistence.payment.repository

import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class PaymentCustomRepositoryImpl (
    @PersistenceContext
    private val entityManager: EntityManager
) : PaymentCustomRepository {

    override fun findPayments(
        partnerId: Long?,
        status: String?,
        fromAt: Instant?,
        toAt: Instant?,
        cursorCreatedAt: Instant?,
        cursorId: Long?,
        pageable: Pageable
    ): List<PaymentEntity> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(PaymentEntity::class.java)
        val root = query.from(PaymentEntity::class.java)
        
        val predicates = mutableListOf<Predicate>()
        
        // partnerId 조건
        partnerId?.let { predicates.add(cb.equal(root.get<Long>("partnerId"), it)) }
        
        // status 조건
        status?.let { predicates.add(cb.equal(root.get<String>("status"), it)) }
        
        // fromAt 조건
        fromAt?.let { predicates.add(cb.greaterThanOrEqualTo(root.get<Instant>("createdAt"), it)) }
        
        // toAt 조건
        toAt?.let { predicates.add(cb.lessThan(root.get<Instant>("createdAt"), it)) }
        
        // 커서 조건 (createdAt, id 순으로 정렬)
        if (cursorCreatedAt != null && cursorId != null) {
            predicates.add(
                cb.or(
                    cb.lessThan(root.get<Instant>("createdAt"), cursorCreatedAt),
                    cb.and(
                        cb.equal(root.get<Instant>("createdAt"), cursorCreatedAt),
                        cb.lessThan(root.get<Long>("id"), cursorId)
                    )
                )
            )
        }
        
        query.where(*predicates.toTypedArray())
        query.orderBy(
            cb.desc(root.get<Instant>("createdAt")),
            cb.desc(root.get<Long>("id"))
        )
        
        val typedQuery: TypedQuery<PaymentEntity> = entityManager.createQuery(query)
        typedQuery.firstResult = pageable.pageNumber * pageable.pageSize
        typedQuery.maxResults = pageable.pageSize
        
        return typedQuery.resultList
    }
}