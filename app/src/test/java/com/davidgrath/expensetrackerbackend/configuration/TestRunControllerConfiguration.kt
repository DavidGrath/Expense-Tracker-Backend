package com.davidgrath.expensetrackerbackend.configuration

import com.davidgrath.expensetrackerbackend.configuration.filter.TokenTypeFilter
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
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
    TransactionRepository::class, TestTimeHandler::class, TokenTypeFilter::class])
open class TestRunControllerConfiguration: TestRunConfiguration() {

}