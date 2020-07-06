package com.hk.voting.test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant


/**
 * test APIs configuration
 */
@RestController
class HelloController @Autowired constructor(
        private val env: Environment
) {


    @GetMapping("/api/v1/test/hello")
    fun hello(): String {
        return "hello"
    }

    @GetMapping("/api/v1/test/foo")
    fun foo(): Foo {
        return Foo(123, "abc", Instant.parse("2018-05-30T21:12:15.42919Z"))
    }

    @GetMapping("/api/v1/test/profile")
    fun profile(): String? {
        return env.getProperty("profile")
    }

    @Secured(*["ROLE_ADMIN"])
    @GetMapping("/admin/api/v1/test/admin")
    fun roleAdmin(): String {
        val principle = SecurityContextHolder.getContext().authentication?.principal as UserDetails
        return principle.username
    }

    @Secured(*["ROLE_ADMIN", "ROLE_EDITOR"])
    @GetMapping("/admin/api/v1/test/editor")
    fun roleEditor(): String {
        val principle = SecurityContextHolder.getContext().authentication?.principal as UserDetails
        return principle.username
    }


}
