package com.davidgrath.expensetrackerbackend.configuration

import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

class SimpleRateLimitFilter(private val bucket: Bucket): OncePerRequestFilter() {


    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        bucket.asBlocking().tryConsumeUninterruptibly(1, Duration.ofSeconds(10))
        filterChain.doFilter(request, response)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SimpleRateLimitFilter::class.java)
    }
}