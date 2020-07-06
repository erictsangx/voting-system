package com.hk.voting.services

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.hk.voting.models.AuthUser
import com.hk.voting.models.Role
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*


@Service
class JwtService constructor(
        @Value("\${jwt.secret}") private val secret: String
) {

    companion object {
        private const val issuer: String = "voting-system"
    }

    private val logger = LoggerFactory.getLogger(JwtService::class.java)

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)


    private val verifier: JWTVerifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .build() //Reusable verifier instance


    fun sign(user: AuthUser): String? {
        return try {
            val exp = Date.from(Instant.now() + Duration.ofHours(2))
            JWT.create()
                    .withIssuer(issuer)
                    .withClaim(AuthUser::id.name, user.id)
                    .withClaim(AuthUser::username.name, user.username)
                    .withArrayClaim(AuthUser::roles.name, user.roles.map { it.name }.toTypedArray())
                    .withClaim("exp", exp)
                    .sign(algorithm)
        } catch (exception: JWTCreationException) {
            //Invalid Signing configuration / Couldn't convert Claims.
            logger.warn("Unable to sign JWT", exception)
            null
        }
    }

    fun verify(token: String): AuthUser? {
        try {
            val jwt: DecodedJWT = verifier.verify(token)

            val id = jwt.getClaim(AuthUser::id.name).asString()
            val name = jwt.getClaim(AuthUser::username.name).asString()
            val roles = jwt.getClaim(AuthUser::roles.name).asArray(String::class.java)?.map { Role.valueOf(it) }

            return if (id != null && name != null && roles != null) {
                AuthUser(id, name, "", roles)
            } else null

        } catch (exception: JWTVerificationException) {
            //Invalid signature/claims
            logger.warn(exception.localizedMessage)
            return null
        }
    }
}
