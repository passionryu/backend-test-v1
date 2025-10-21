package im.bigs.pg.application.partner.manager

import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.domain.partner.Partner
import org.springframework.stereotype.Component

/**
 * Partner 도메인 관련 비즈니스 로직을 담당하는 매니저 클래스.
 * - Partner 조회
 * - 활성화 여부 검증
 */
@Component
class PartnerManager(
    private val partnerRepository: PartnerOutPort
) {
    /**
     * 특정 파트너를 조회하고, 활성화 상태인지 확인합니다.
     *
     * @param partnerId 조회할 파트너 ID
     * @return Partner 도메인 객체
     * @throws IllegalArgumentException 파트너가 존재하지 않을 경우
     * @throws IllegalStateException 파트너가 비활성화 상태일 경우
     */
    fun findPartner(partnerId: Long): Partner {
        val partner = partnerRepository.findById(partnerId)
            ?: throw IllegalArgumentException("Partner not found: $partnerId")

        // 비활성화된 파트너는 결제 처리 불가
        require(partner.active) { "Partner is inactive: ${partner.id}" }

        return partner
    }
}