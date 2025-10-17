package im.bigs.pg.external.pg.dto

/**
 * pgserver에서 오는 응답을 역직렬화 하는 DTO
 */
data class PgApproveApiResponse(
    val approvalCode: String,
    val approvedAt: String,
    val maskedCardLast4: String?,
    val amount: Long?,
    val status: String
)