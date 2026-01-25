package com.davidgrath.expensetrackerbackend.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
open class SecurityConfiguration {

    @Autowired
    lateinit var timeHandler: TimeHandler


    @Bean
    @Order(1)
    open fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        httpSecurity
            .securityMatcher("/auth/signUp")
            .csrf().disable()
            .authorizeHttpRequests { c ->
                c.anyRequest().permitAll()
            }

        return httpSecurity.build()
    }

    @Bean
    @Order(2)
    open fun basicAuthFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        httpSecurity
            .securityMatcher("/auth/login")
            .csrf().disable()
            .authorizeHttpRequests { c ->
                c.anyRequest().authenticated()
            }.httpBasic(Customizer.withDefaults())
        return httpSecurity.build()
    }

    @Bean
    @Order(3)
    open fun bearerAuthFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        httpSecurity
            .csrf().disable()
            .authorizeHttpRequests { c ->
                c.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            }
        return httpSecurity.build()
    }

    @Bean
    open fun customUserDetailsService(): UserDetailsService {
        return CustomUserDetailsService()
    }
}