package com.hk.voting.models

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

@Document
enum class Role {
    ADMIN,
    EDITOR
}

@Document
data class AuthUser(
        val id: String,
        val username: String,
        @JsonIgnore
        val password: String,
        val roles: List<Role>
) {
    fun toUserDetails(): UserDetails {
        return User(username, password, roles.map { SimpleGrantedAuthority("ROLE_$it") })
    }
}
