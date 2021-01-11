package com.psachdev

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.application.ApplicationEnvironment
import io.ktor.util.KtorExperimentalAPI
import java.util.*

class JwtManager {

    private lateinit var issuer: String
    private lateinit var secret: String
    private lateinit var validityInMs: String
    private lateinit var algorithm: Algorithm

    lateinit var verifier: JWTVerifier

    @KtorExperimentalAPI
    fun init(environment: ApplicationEnvironment){
        issuer = environment.config.property("jwt.issuer").getString()
        secret = environment.config.property("jwt.secret").getString()
        algorithm = Algorithm.HMAC512(secret)
        validityInMs = environment.config.property("jwt.validity_ms").getString()

        verifier = JWT
            .require(algorithm)
            .withIssuer(issuer)
            .build()
    }


    fun makeToken(user: User): String = JWT.create()
        .withSubject(tokenSubject)
        .withIssuer(issuer)
        .withClaim(tokenClaimId, user.id)
        .withClaim(tokenClaimName, user.username)
        .withExpiresAt(getExpiration())
        .sign(algorithm)


    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs.toLong())

    companion object {
        private const val tokenSubject = "Authentication"
        internal const val tokenClaimId = "id"
        private const val tokenClaimName = "name"
    }
}
