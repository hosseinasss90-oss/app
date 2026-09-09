package ir.hesabino.app.engine.fingerprint

import ir.hesabino.app.engine.normalize.NumberNormalizer
import java.security.MessageDigest

/**
 * اثرانگشت پایدار برای جلوگیری از ثبت تکراری BankEvent.
 *
 * timeBucket = دقیقهٔ دریافت — SMS تکراری بانک معمولاً در همان دقیقه است.
 * خط «مانده» از بدنه حذف می‌شود چون با هر تراکنش عوض می‌شود و نباید هویت رویداد را عوض کند.
 */
object Fingerprint {

    const val TIME_BUCKET_MS = 60_000L

    fun compute(
        senderId: String,
        amountRials: Long?,
        cardLast4: String?,
        receivedAtMillis: Long,
        rawBody: String,
    ): String {
        val sender = NumberNormalizer.normalizeText(senderId).lowercase()
        val amount = amountRials?.toString() ?: "?"
        val last4 = cardLast4?.filter { it.isDigit() }?.takeLast(4).orEmpty().ifBlank { "-" }
        val bucket = receivedAtMillis / TIME_BUCKET_MS
        val body = NumberNormalizer.canonicalizeBodyForFingerprint(rawBody)
        val material = "$sender|$amount|$last4|$bucket|$body"
        return sha256(material)
    }

    fun bodyHash(rawBody: String): String =
        sha256(NumberNormalizer.canonicalizeBodyForFingerprint(rawBody))

    fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
