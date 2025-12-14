package com.davidgrath.expensetrackerbackend.configuration

import com.davidgrath.expensetrackerbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

class CustomUserDetailsService: UserDetailsService {

    @Autowired
    lateinit var userService: UserService

    override fun loadUserByUsername(username: String?): UserDetails? {
        if(username == null) {
            return null
        }
        val user = userService.getByUsername(username)
        if(user == null) {
            return null
        }
        return CustomUserDetails(user)
    }
}