package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        AuthService::class
    ])
class AuthServiceTest {

    @Autowired
    lateinit var authService: AuthService

    /**
     * /auth/login also has some terrible p95 (10 seconds) on my JMeter, so I'm trying to see if this fixes it
     * UPDATE: Solution: threadPool of size `processorCount`
     * ANOTHER UPDATE: BCrypt still takes up the majority of processor time. `createUserToken` was improved, though. That's okay for me
     */
    @Test
    @Ignore("Performance test")
    fun createUsernameTest() {
        val threadPool = Executors.newFixedThreadPool(400) //Simulating the server thread pool
        val startTimeNanos = System.nanoTime()
        for (i in 1..400) {
            threadPool.submit {
                authService.createUserToken("test")
            }
        }
        threadPool.shutdown()
        val didNotTimeOut = threadPool.awaitTermination(20, TimeUnit.SECONDS)
        LOGGER.info("Did not time out: {}", didNotTimeOut)
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