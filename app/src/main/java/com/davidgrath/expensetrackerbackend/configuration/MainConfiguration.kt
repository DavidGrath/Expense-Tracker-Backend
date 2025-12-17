package com.davidgrath.expensetrackerbackend.configuration


import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.AuthService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.sql.SQLException
import java.time.ZoneId
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.sql.DataSource

@Configuration
class MainConfiguration {

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    private val JWT_ALG = "HmacSHA256"
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
        return HikariDataSource(hikariConfig)
    }

    @Bean
    open fun dslContext(hikariDataSource: DataSource): DSLContext {
        LOGGER.debug("Hikari: {}", hikariDataSource == null)
        val dialect = SQLDialect.MYSQL
        val c = DSL.using(hikariDataSource, dialect)
        LOGGER.debug("DSL Context: {}", c)
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
        val key: SecretKey = SecretKeySpec(jwtSigningKey!!.toByteArray(), JWT_ALG)
        return NimbusJwtDecoder.withSecretKey(key).build()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainConfiguration::class.java)
    }

}