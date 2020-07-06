package com.hk.voting.utility

import com.hk.voting.models.AuthUser
import com.hk.voting.models.Role
import com.hk.voting.services.JwtService
import com.hk.voting.test.randomId
import com.hk.voting.test.randomStr
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


class JwtServiceTest {

    private val jwtUtil = JwtService("accR26COqEaAVZPqeoze+kEFZVdj7TbipAdcwirmTL8=")

    @Test
    fun `sign and verify`() {

        val user = AuthUser(randomId, randomStr, randomStr, listOf(Role.ADMIN))
        val token = jwtUtil.sign(user)
        assertTrue(token != null)

        val decoded = jwtUtil.verify(token!!)
        assertTrue(decoded != null)
        assertEquals(user.id, decoded!!.id)
        assertEquals(user.username, decoded.username)
        assertEquals("", decoded.password)
        assertEquals(user.roles, decoded.roles)

    }

    @Test
    fun `wrong key`() {
        val wrongSecret = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJBRE1JTiJdLCJpc3MiOiJ2b3Rpbmctc3lzdGVtIiwiaWQiOiI1ZjAxYzczMzg0YTRmYjIyNzA3MTRjZDciLCJleHAiOjE1OTM5NTkyNTEsInVzZXJuYW1lIjoiYzJlMjc5MjkifQ.P_AJydiQxRtYu8v940kjbXsJm-EvGPMTYuZYV5jGCag"

        assertNull(jwtUtil.verify(wrongSecret))
    }

    @Test
    fun `missing fields`() {
        val missingUserId = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJBRE1JTiJdLCJpc3MiOiJ2b3Rpbmctc3lzdGVtIiwidXNlcm5hbWUiOiIzYWIyNzYzMiJ9.nZiIwgqUp2tnUEX2Vsa3cvqV1IpvfLxKepyi9qa5MkU"

        assertNull(jwtUtil.verify(missingUserId))
    }
}
