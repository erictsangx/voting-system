package com.hk.voting.utility

import com.hk.voting.test.randomBool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder


class MathTest {

    @Test
    fun `validate HK ID`() {
        listOf(
                "WO790393A",
                "M6884593",
                "U6944954",
                "K6050549",
                "F9463786",
                "X3859512",
                "PF3046059",
                "WF1871640",
                "WX7903934"
        ).map {
            if (randomBool) {
                it.toLowerCase()
            } else {
                it
            }
        }.forEach {
            assertTrue(validateHKID(it))
        }
    }

    @Test
    fun `hash string`() {
        val hashed = privacyHash("WF1871640")
        assertEquals("LckgaB9wCdzLVWjT43sJSsBxnZF47u0evPIiE8TQrXP3cQN+1rKEL33OaahWQfnjaD5IB+sgPVIevj+8AISfAg==", hashed)
    }

}
