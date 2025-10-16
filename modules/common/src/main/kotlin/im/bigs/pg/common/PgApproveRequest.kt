package im.bigs.pg.common

import java.math.BigDecimal

/** PG 승인 요청 최소 정보. */
data class PgApproveRequest(
    val partnerId: Long,
    val amount: BigDecimal,
    val cardBin: String?,
    val cardLast4: String?,
    val birthDate: String,
    val expiry: String,
    val password: String,
    val productName: String?,
)
