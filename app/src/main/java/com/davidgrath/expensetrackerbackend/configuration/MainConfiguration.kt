package com.davidgrath.expensetrackerbackend.configuration


import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.sql.SQLException
import java.time.Duration
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource

@Configuration
class MainConfiguration {

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Value("\${jwt.publickey}")
    private val jwtPublicKey: String? = null

    @Bean
    fun timeHandler(): TimeHandler {
        return MainTimeHandler()
    }
    @Bean
    @Throws(SQLException::class)
    fun hikariDataSource(
        @Value("\${spring.datasource.url}") url: String?,
        @Value("\${spring.datasource.username}") username: String?,
        @Value("\${spring.datasource.password}") password: String?
    ): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = url
        hikariConfig.username = username
        hikariConfig.password = password
        hikariConfig.addDataSourceProperty("connectionTimeZone", ZoneId.systemDefault())
//        hikariConfig.addDataSourceProperty("useAffectedRows", "true") //failed attempt to make JDBC return 0 instead of 1 for idempotent single row updates
        hikariConfig.maxLifetime = Duration.ofMinutes(10).toMillis()
        return HikariDataSource(hikariConfig)
    }

    @Bean
    open fun dslContext(hikariDataSource: DataSource): DSLContext {
        val dialect = SQLDialect.MYSQL
        //Pro - Just one log statement instead of individual log statements
        //Con - Class and method names get lost
//        val listener = DefaultExecuteListenerProvider(LoggingExecutionListener())
        val configuration = DefaultConfiguration()
            .set(hikariDataSource)
            .set(dialect)
//            .set(listener)
        val c = DSL.using(configuration)
        return c
    }

    val BCRYPT_STRENGTH = 12
    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        val bcryptEncoder = BCryptPasswordEncoder(BCRYPT_STRENGTH)
        return DelegatingPasswordEncoder("bcrypt", mapOf("bcrypt" to bcryptEncoder))
    }


    @Bean
    open fun jwtDecoder(): JwtDecoder {
        val bytes = Base64.getDecoder().decode(jwtPublicKey)
        val keySpec = X509EncodedKeySpec(bytes)
        val keyFactory = KeyFactory.getInstance(JWT_ALG)
        val pubKey = keyFactory.generatePublic(keySpec) as RSAPublicKey
        return NimbusJwtDecoder.withPublicKey(pubKey).signatureAlgorithm(SignatureAlgorithm.RS512) .build()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainConfiguration::class.java)
    }

}