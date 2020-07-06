package com.hk.voting.utility

import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


val nextHour: Instant
    get() = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong")).truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant()


/**
 * Hash without salting, SHOULD NOT USE FOR HASHING PASSWORD
 * @return base64 encoded and hashed string
 */
fun privacyHash(str: String): String {
    val charset = Charsets.UTF_8
    val md: MessageDigest = MessageDigest.getInstance("SHA-512")
    val hashed = md.digest(str.toByteArray(charset))
    return Base64.getEncoder().encodeToString(hashed)
}


/**
 * @param id case-insensitive, no parentheses, e.g. K6050549
 */
fun validateHKID(id: String): Boolean {
    val capitalized = id.toUpperCase()

    val reg = Regex("^[a-zA-Z]{1,2}[0-9]{6}[0-9aA]$")
    if (reg.matches(capitalized)) {
        val match = Regex("([a-zA-Z]{1,2})([0-9]{6}[0-9aA])").find(capitalized)!!
        val (alphabet, digits) = match.destructured
        var sum = 0

        /**
         * SPACE value for single alphabet id = 36
         */
        if (alphabet.length == 1) {
            sum += 36 * 9
            sum += alphabetToInt(alphabet[0]) * 8
        } else {
            sum += alphabetToInt(alphabet[0]) * 9
            sum += alphabetToInt(alphabet[1]) * 8
        }
        digits.forEachIndexed { index, c ->
            sum += digitToInt(c) * (7 - index)
        }
        return sum % 11 == 0
    } else {
        return false
    }
}

/**
 * For HK ID, A=10,B=11,...Z=35
 * ASCII A=65,B=66,..Z=90
 */
private fun alphabetToInt(c: Char): Int = (c.toInt() - 55)

/**
 * For check digit A=10
 * ASCII 9=57,8=56...
 */
private fun digitToInt(num: Char): Int {
    return if (num == 'A') {
        10
    } else num.toInt() - '0'.toInt()
}
