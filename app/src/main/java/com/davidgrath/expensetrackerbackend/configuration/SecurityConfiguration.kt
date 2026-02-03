package com.davidgrath.expensetrackerbackend.configuration

import com.davidgrath.expensetrackerbackend.configuration.filter.TokenTypeFilter
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.time.Duration

@Configuration
@EnableWebSecurity
open class SecurityConfiguration {

    @Autowired
    lateinit var timeHandler: TimeHandler

    @Autowired
    @Qualifier("handlerExceptionResolver")
    lateinit var handlerExceptionResolver: HandlerExceptionResolver


    /*val loginBucket = Bucket
        .builder()
        .addLimit { limit ->
            limit.capacity(9).refillIntervally(9, Duration.ofSeconds(1))
        }
        .withListener(CustomBucketListener())
        .build()*/



    @Bean
    @Order(1)
    open fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        httpSecurity
            .securityMatcher("/auth/signUp", "/auth/refresh")
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
//            .addFilterAfter(SimpleRateLimitFilter(loginBucket), LogoutFilter::class.java)
        return httpSecurity.build()
    }

    @Bean
    @Order(3)
    open fun bearerAuthFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        val tokenTypeFilter = TokenTypeFilter(handlerExceptionResolver)
        httpSecurity
            .csrf().disable()
            .authorizeHttpRequests { c ->
                c.anyRequest().authenticated()
            }
            .addFilterAfter(tokenTypeFilter, AnonymousAuthenticationFilter::class.java)
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