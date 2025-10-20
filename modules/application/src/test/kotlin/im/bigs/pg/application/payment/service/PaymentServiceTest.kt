package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.manager.FeePolicyManager
import im.bigs.pg.application.partner.manager.PartnerManager
import im.bigs.pg.application.payment.manager.PaymentAuthorizationManager
import im.bigs.pg.application.payment.manager.PaymentBuilder
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.manager.PgClientManager
import io.mockk.mockk

class PaymentServiceTest {

    private val partnerManager = mockk<PartnerManager>()
    private val pgClientManager = mockk<PgClientManager>()
    private val paymentAuthorizationManager = mockk<PaymentAuthorizationManager>()
    private val feePolicyManager = mockk<FeePolicyManager>()
    private val paymentBuilder = mockk<PaymentBuilder>()
    private val paymentRepository = mockk<PaymentOutPort>()

    private val paymentService = PaymentService(
        partnerManager = partnerManager,
        pgClientManager = pgClientManager,
        paymentAuthorizationManager = paymentAuthorizationManager,
        feePolicyManager = feePolicyManager,
        paymentBuilder = paymentBuilder,
        paymentRepository = paymentRepository
    )

}
