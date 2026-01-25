package com.davidgrath.expensetrackerbackend.configuration

import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
open class WebMvcConfiguration : WebMvcConfigurer {


    override fun addFormatters(registry: FormatterRegistry) {
        // Needed for converting controller parameters to enums properly
        ApplicationConversionService.configure(registry)
    }
}
