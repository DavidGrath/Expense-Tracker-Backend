package com.davidgrath.expensetrackerbackend.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import java.util.*

@Configuration
@EnableWebSecurity
open class SecurityConfiguration {

    @Autowired
    lateinit var timeHandler: TimeHandler


    @Bean
    open fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        httpSecurity
            .csrf().disable()
            .authorizeHttpRequests { c ->
                c.requestMatchers("/auth/login").authenticated()
            }.httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests { c ->
            c
                .requestMatchers("/auth/signUp").permitAll()
                .anyRequest().authenticated()
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