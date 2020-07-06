package com.hk.voting.controllers

import com.hk.voting.exceptions.LoginException
import com.hk.voting.models.AuthLogin
import com.hk.voting.services.AuthUserRepo
import com.hk.voting.services.JwtService
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
@RequestMapping("/api/v1/auth")
class AuthController @Autowired constructor(
        private val authUserRepo: AuthUserRepo,
        private val jwt: JwtService,
        private val argon2: PasswordEncoder
) {

    @ApiOperation("Login a user and return JWT in response header", notes = "expires in 2 hours")
    @PostMapping("/login")
    fun login(@RequestBody authLogin: AuthLogin, request: HttpServletRequest, response: HttpServletResponse) {
        val user = authUserRepo.singleOrNull(authLogin.username)
        return if (user != null && argon2.matches(authLogin.password, user.password)) {
            val token = jwt.sign(user)
            response.setHeader("JWT", token)
        } else {
            request.logout()
            throw LoginException()
        }
    }

}
