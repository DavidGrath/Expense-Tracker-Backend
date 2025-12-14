package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.service.TransactionService
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap


//@Component
class AuthUtils {


//    @Autowired
//    private val userTokenBlacklistRepository: UserTokenBlacklistRepository? = null
//    fun validateJwt(jwt: String): Map<String, Any>? {
//        val secretKey: SecretKey = SecretKeySpec(jwtSigningKey!!.toByteArray(), JWT_ALG)
//        return try {
//            val entrySet: Set<Map.Entry<String, Any>> = Jwts.parser()
//                .verifyWith(secretKey)
//                .build()
//                .parse(jwt)
//                .accept(Jws.CLAIMS)
//                .payload
//                .entries
//            val claims: MutableMap<String, Any> = HashMap()
//            for ((key, value) in entrySet) {
//                claims[key] = value
//            }
//            val jti = claims["jti"] as String?
//            val isTokenInBlacklist =
//                if (jti != null) userTokenBlacklistRepository.getTokenBlacklistEntryByJti(jti).isNotEmpty() else false
//            if (isTokenInBlacklist) null else claims
//            claims
//        } catch (e: RuntimeException) {
//            LOGGER.error("Problem parsing JWT: ", e)
//            null
//        }
//    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthUtils::class.java)
    }
}
