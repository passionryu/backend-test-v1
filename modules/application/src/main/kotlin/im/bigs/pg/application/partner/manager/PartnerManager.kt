package im.bigs.pg.application.partner.manager

import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.domain.partner.Partner
import org.springframework.stereotype.Component

@Component
class PartnerManager(
    private val partnerRepository: PartnerOutPort
) {
    fun findPartner(partnerId: Long): Partner {
        val partner = partnerRepository.findById(partnerId)
            ?: throw IllegalArgumentException("Partner not found: $partnerId")
        require(partner.active) { "Partner is inactive: ${partner.id}" }
        return partner
    }
}