package com.davidgrath.expensetrackerbackend.configuration

import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.testutils.TestTimeHandler
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.sql.SQLException
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource

@TestConfiguration
@ComponentScan(basePackageClasses = [
    TransactionService::class,
    TransactionRepository::class, TestTimeHandler::class])
open class TestRunConfiguration {

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Value("\${jwt.publickey}")
    private val jwtPublicKey: String? = null

    @Bean
    open fun timeHandler(): TimeHandler {
        return TestTimeHandler()
    }

    @Bean
    @Throws(SQLException::class)
    open fun hikariDataSource(
        @Value("\${spring.datasource.url}") url: String?,
        @Value("\${spring.datasource.username}") username: String?,
        @Value("\${spring.datasource.password}") password: String?
    ): DataSource? {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = url
        hikariConfig.username = username
        hikariConfig.password = password
        hikariConfig.addDataSourceProperty("connectionTimeZone", ZoneId.systemDefault())
        return HikariDataSource(hikariConfig)
    }

    @Bean
    open fun dslContext(hikariDataSource: DataSource?): DSLContext? {
        val dialect = SQLDialect.MYSQL
        //Refer to MainConfiguration
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
        val passwordEncoder = NoOpPasswordEncoder.getInstance()
        return passwordEncoder
    }


    @Bean
    open fun jwtDecoder(): JwtDecoder {
        val bytes = Base64.getDecoder().decode(jwtPublicKey)
        val keySpec = X509EncodedKeySpec(bytes)
        val keyFactory = KeyFactory.getInstance(JWT_ALG)
        val pubKey = keyFactory.generatePublic(keySpec) as RSAPublicKey


        return NimbusJwtDecoder.withPublicKey(pubKey).signatureAlgorithm(SignatureAlgorithm.RS512).build()
    }

}