package com.davidgrath.expensetrackerbackend.configuration.filter

import io.opentelemetry.api.trace.Span
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class MDCClearFilter: OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request,response)
        } finally {
            LOGGER.debug("MDC Value: {}", MDC.get("extrack.user_uuid"))
            LOGGER.debug("MDC Value: {}", MDC.get("extrack.profile_uuid"))
            MDC.clear()
            LOGGER.debug("Cleared")
            LOGGER.debug("MDC Value: {}", MDC.get("extrack.user_uuid"))
            LOGGER.debug("MDC Value: {}", MDC.get("extrack.profile_uuid"))
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MDCClearFilter::class.java)
    }
}