package im.bigs.pg.application.payment.helper

import im.bigs.pg.domain.payment.PaymentStatus

/**
 * 문자열 값을 [PaymentStatus] enum으로 안전하게 변환하는 매퍼.
 *
 * 잘못된 문자열이 들어오더라도 예외를 던지지 않고 `null`을 반환한다.
 * 주로 외부 요청 파라미터나 DB 문자열 값을 enum으로 변환할 때 사용한다.
 */
object PaymentStatusMapper {

    /**
     * 주어진 문자열을 [PaymentStatus]로 변환한다.
     *
     * 대소문자를 구분하지 않으며, 유효하지 않은 값인 경우 `null`을 반환한다.
     *
     * @param value 변환할 문자열 (예: `"APPROVED"`, `"canceled"`)
     * @return 대응되는 [PaymentStatus] 또는 `null`
     *
     * 예시:
     * ```
     * from("approved") → PaymentStatus.APPROVED
     * from("INVALID") → null
     * ```
     */
    fun from(value: String?): PaymentStatus? {
        return try {
            value?.let { PaymentStatus.valueOf(it.uppercase()) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}