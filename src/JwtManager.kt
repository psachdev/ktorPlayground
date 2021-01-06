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
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    @KtorExperimentalAPI
    fun init(environment: ApplicationEnvironment){
        issuer = environment.config.property("jwt.domain").getString()
        secret = environment.config.property("jwt.secret").getString()
        validityInMs = environment.config.property("jwt.validity_ms").getString()
    }


    fun makeToken(user: SignalUser): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("id", user.id)
        .withClaim("name", user.username)
        .withExpiresAt(getExpiration())
        .sign(algorithm)

    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs.toLong())
}
