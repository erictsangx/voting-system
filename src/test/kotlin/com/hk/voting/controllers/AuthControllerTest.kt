package com.hk.voting.controllers

import com.hk.voting.models.AuthLogin
import com.hk.voting.models.AuthUser
import com.hk.voting.models.Role
import com.hk.voting.services.AuthUserRepo
import com.hk.voting.services.JwtService
import com.hk.voting.test.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc


@WebMvcTest(*[AuthController::class, JwtService::class], properties = ["embedded.containers.enabled=false"])
@TestPropertySource(locations = ["classpath:application.test.properties"])
class AuthControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @MockBean
    lateinit var userRepo: AuthUserRepo

    @Autowired
    lateinit var argon2: PasswordEncoder

    @Autowired
    lateinit var jwtService: JwtService

    @Test
    fun login() {
        val password = randomStr
        val user = AuthUser(randomId, randomStr, argon2.encode(password), listOf(Role.ADMIN))
        method(userRepo.singleOrNull(user.username)) {
            user
        }
        val response = mvc.post("/api/v1/auth/login", AuthLogin(user.username, password))
                .andExpect(status.isOk)
                .andReturn().response.getHeader("JWT")!!

        val verified = jwtService.verify(response)!!
        assertEquals(user.id, verified.id)
        assertEquals(user.username, verified.username)
        assertEquals(user.roles, verified.roles)
        assertEquals("", verified.password)
    }
}
