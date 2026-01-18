package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.time.Duration

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestConfiguration::class,
        AuthService::class
    ])
class AuthServiceTest {

    @Autowired
    lateinit var authService: AuthService

    /**
     * /auth/login also has some terrible p95 (10 seconds) on my JMeter, so I'm trying to see if this fixes it
     */
    @Test
    fun createUsernameTest() {
        val startTimeNanos = System.nanoTime()
        for (i in 1..150) {
            authService.createUserToken("test")
        }
        val endTimeNanos = System.nanoTime()
        val d = endTimeNanos - startTimeNanos
        val duration = Duration.ofNanos(d)
        LOGGER.debug("Create tokens time: {}", duration.toString())
        val durationSeconds = Duration.ofNanos(d).toSeconds()
        assertTrue(durationSeconds <= 10)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthServiceTest::class.java)
    }
}