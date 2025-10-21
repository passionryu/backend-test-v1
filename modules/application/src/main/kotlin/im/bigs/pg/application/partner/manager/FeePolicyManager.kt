package im.bigs.pg.application.partner.manager

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.domain.partner.FeePolicy
import org.springframework.stereotype.Component

/**
 * Partner별 수수료 정책 조회를 담당하는 매니저
 * - 유효한 정책 조회
 * - 정책 미존재 시 예외 처리
 */
@Component
class FeePolicyManager(
    private val feePolicyRepository: FeePolicyOutPort
) {
    /**
     * 특정 Partner의 유효한 수수료 정책을 조회합니다.
     *
     * @param partnerId 조회할 Partner ID
     * @return FeePolicy 도메인 객체
     * @throws IllegalStateException 정책이 존재하지 않을 경우
     */
    fun findFeePolicy(partnerId: Long): FeePolicy {
        return feePolicyRepository.findEffectivePolicy(partnerId)
            ?: throw IllegalStateException("No fee policy found for partner: $partnerId")
    }
}
