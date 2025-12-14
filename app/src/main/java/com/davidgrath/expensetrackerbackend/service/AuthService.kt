package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Service
class AuthService {


    @Autowired
    lateinit var timeHandler: TimeHandler

    private val JWT_ALG = "HmacSHA256"

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null

    fun createUserToken(username: String): String {

        val key: SecretKey = SecretKeySpec(jwtSigningKey!!.toByteArray(), JWT_ALG)
        val localDateTime = LocalDateTime.now(timeHandler.getClock())
        val expiry = localDateTime.plus(60, ChronoUnit.MINUTES)
        val jws = Jwts.builder()
            .subject(username)
            .signWith(key)
            .issuedAt(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
            .expiration(Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant()))
            .id(UUID.randomUUID().toString())
            .compact()
        return jws

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthService::class.java)
    }

}