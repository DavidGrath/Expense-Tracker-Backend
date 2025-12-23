package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
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


    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null

    fun createUserToken(username: String): String {

        val bytes = Base64.getDecoder().decode(jwtSigningKey)
        val keySpec = PKCS8EncodedKeySpec(bytes)
        val keyFactory = KeyFactory.getInstance(JWT_ALG)
        val key = keyFactory.generatePrivate(keySpec) as RSAPrivateKey
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