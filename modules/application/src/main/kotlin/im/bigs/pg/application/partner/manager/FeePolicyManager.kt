package im.bigs.pg.application.partner.manager

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.domain.partner.FeePolicy
import org.springframework.stereotype.Component

@Component
class FeePolicyManager(
    private val feePolicyRepository: FeePolicyOutPort
) {
    fun findFeePolicy(partnerId: Long): FeePolicy {
        return feePolicyRepository.findEffectivePolicy(partnerId)
            ?: throw IllegalStateException("No fee policy found for partner: $partnerId")
    }
}
