package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.configuration.TimeAndLocaleHandler
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.sql.SQLException
import java.time.ZoneId
import javax.sql.DataSource

@TestConfiguration
@ComponentScan(basePackageClasses = [com.davidgrath.expensetrackerbackend.testutils.TestConfiguration::class,
    TransactionService::class,
    TransactionRepository::class, TestTimeAndLocaleHandler::class])
open class TestConfiguration {
    @Bean
    open fun timeAndLocaleHandler(): TimeAndLocaleHandler {
        return TestTimeAndLocaleHandler()
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
        return DSL.using(hikariDataSource, dialect)
    }
}