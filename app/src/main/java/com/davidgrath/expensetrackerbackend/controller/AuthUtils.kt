package com.davidgrath.expensetrackerbackend.controller

import org.slf4j.LoggerFactory


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
