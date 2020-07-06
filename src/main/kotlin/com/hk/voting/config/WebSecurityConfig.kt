package com.hk.voting.config

import com.hk.voting.services.JwtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var jwtService: JwtService

    override fun configure(http: HttpSecurity) {
        // no need csrf for jwt
        http.csrf().disable()
                .formLogin().disable()
                .authorizeRequests()
                .antMatchers("/admin/**").fullyAuthenticated()
                .anyRequest().permitAll()
                .and()
                .addFilterBefore(TokenAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
                .exceptionHandling()
                .accessDeniedHandler(CustomAccessDeniedHandler())
                .authenticationEntryPoint(CustomAuthenticationEntryPoint())

    }

    //argon2id version
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return Argon2PasswordEncoder()
    }
}


class CustomAccessDeniedHandler : AccessDeniedHandler {

    companion object {
        const val forbidden = """{"result":null,"code":403,"message":"Permission Denied"}"""
    }

    private val logger = LoggerFactory.getLogger(CustomAccessDeniedHandler::class.java)

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: org.springframework.security.access.AccessDeniedException) {
        logger.error("Permission Denied", accessDeniedException)
        response.status = 403
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.print(forbidden)
    }

}


class CustomAuthenticationEntryPoint : AuthenticationEntryPoint {

    companion object {
        const val Unauthorized = """{"result":null,"code":401,"message":"Unauthorized"}"""
    }

    private val logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint::class.java)

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        logger.error(authException.message)
        response.status = 401
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.print(Unauthorized)
    }
}


class TokenAuthenticationFilter(private val jwtService: JwtService) : GenericFilterBean() {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest

        val accessToken = httpRequest.getHeader("JWT")
        if (accessToken != null) {
            val authUser = jwtService.verify(accessToken)
            if (authUser != null) {
                val userDetails = authUser.toUserDetails()
                val authentication = UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
                SecurityContextHolder.getContext().authentication = authentication
            } else {
                SecurityContextHolder.getContext().authentication = null
            }
        } else {
            //reset a user's authentication if the user is authenticated but token is lost
            if (SecurityContextHolder.getContext().authentication != null) {
                SecurityContextHolder.getContext().authentication = null
            }
        }
        chain.doFilter(request, response)
    }
}
