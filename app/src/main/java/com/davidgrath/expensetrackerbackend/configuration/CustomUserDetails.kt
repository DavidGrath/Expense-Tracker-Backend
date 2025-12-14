package com.davidgrath.expensetrackerbackend.configuration

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(private val user: UserPojo): UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority?>? {
        return emptyList()
    }

    override fun getPassword(): String? {
        return user.password
    }

    override fun getUsername(): String? {
        return user.username
    }
}