package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generatedapi.model.AuthResponse
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class AuthService constructor(
    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String,
    @Value("\${jwt.publickey}")
    private val jwtPublicKey: String
) {


    @Autowired
    lateinit var timeHandler: TimeHandler

    val processorCount = Runtime.getRuntime().availableProcessors()
    val jwtExecutorService: ExecutorService
    init {

        LOGGER.info("Processor count: {}", processorCount)
        jwtExecutorService = Executors.newFixedThreadPool(processorCount)
        LOGGER.info("Created fixed thread pool with size {}", processorCount)
    }

    val key: RSAPrivateKey
    val publicKey: RSAPublicKey
    init {
        var startTime = System.nanoTime()
        val bytes = Base64.getDecoder().decode(jwtSigningKey)
        val keySpec = PKCS8EncodedKeySpec(bytes)
        val keyFactory = KeyFactory.getInstance(JWT_ALG)
        key = keyFactory.generatePrivate(keySpec) as RSAPrivateKey
        var endTime = System.nanoTime()
        var duration = Duration.ofNanos(endTime - startTime)
        LOGGER.info("Time taken to generate private key: {}", duration)

        startTime = System.nanoTime()
        LOGGER.debug("Public key: {}", jwtPublicKey)
        val publicBytes = Base64.getDecoder().decode(jwtPublicKey)
        val publicKeySpec = X509EncodedKeySpec(publicBytes)
        publicKey = keyFactory.generatePublic(publicKeySpec) as RSAPublicKey
        endTime = System.nanoTime()
        duration = Duration.ofNanos(endTime - startTime)
        LOGGER.info("Time taken to generate public key: {}", duration)
    }

    fun createUserToken(username: String): AuthResponse {

        val localDateTime = LocalDateTime.now(timeHandler.getClock())

        val generateJws = Callable<String> {
            val expiry = localDateTime.plus(60, ChronoUnit.MINUTES)
            val startTime = System.nanoTime()
            val jws = Jwts.builder()
                .subject(username)
                .signWith(key)
                .issuedAt(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant()))
                .id(UUID.randomUUID().toString())
                .claim(Constants.CUSTOM_CLAIM_TYPE, Constants.TokenType.Access.value)
                .compact()
            val endTime = System.nanoTime()
            val duration = Duration.ofNanos(endTime - startTime)
            LOGGER.info("Time taken to generate JWS: {}", duration)
            jws
        }
        LOGGER.info("Submitting JWS task to thread pool")
        val future = jwtExecutorService.submit(generateJws)
        val jwt = future.get()
        LOGGER.info("Retrieved JWS")

        val generateRefreshToken = Callable<String> {

            val expiry = localDateTime.plus(1, ChronoUnit.DAYS)
            val startTime = System.nanoTime()
            val refresh = Jwts.builder()
                .subject(username)
                .signWith(key)
                .issuedAt(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant()))
                .id(UUID.randomUUID().toString())
                .claim(Constants.CUSTOM_CLAIM_TYPE, Constants.TokenType.Refresh.value)
                .compact()
            val endTime = System.nanoTime()
            val duration = Duration.ofNanos(endTime - startTime)
            LOGGER.info("Time taken to generate refresh token: {}", duration)
            refresh
        }
        LOGGER.info("Submitting refresh task to thread pool")
        val refreshFuture = jwtExecutorService.submit(generateRefreshToken)
        val refreshToken = refreshFuture.get()
        LOGGER.info("Retrieved refresh token")
        return AuthResponse(jwt, refreshToken)
    }

    fun parseToken(token: String): Jws<Claims> {
        val tokenCallable = Callable<Jws<Claims>> {
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
        }
        val tokenFuture = jwtExecutorService.submit(tokenCallable)
        val token = tokenFuture.get() //I'm not sure if verifying is as computationally expensive as generation, but I'm not taking my chances
        return token
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthService::class.java)
    }

}