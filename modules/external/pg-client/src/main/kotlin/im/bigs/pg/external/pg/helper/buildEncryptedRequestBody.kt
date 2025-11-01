package im.bigs.pg.external.pg.helper

import im.bigs.pg.common.PgApproveRequest
import im.bigs.pg.common.crypto.AesGcmEncryptor

public fun buildEncryptedRequestBody(request: PgApproveRequest, apiKey:String , ivBase64: String): Map<String, String> {
    val plainJson = """
        {
            "cardNumber": "${request.cardBin}-****-****-${request.cardLast4}",
            "birthDate": "${request.birthDate}",
            "expiry": "${request.expiry}",
            "password": "${request.password}",
            "amount": ${request.amount}
        }
    """.trimIndent()

    val enc = AesGcmEncryptor.encryptBase64Url(plainJson, apiKey, ivBase64)
    return mapOf("enc" to enc)
}
