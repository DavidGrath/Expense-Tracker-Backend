package com.davidgrath.expensetrackerbackend.controller;

import com.davidgrath.expensetrackerbackend.generatedapi.api.ProfilesApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.Profile
import com.davidgrath.expensetrackerbackend.configuration.CustomUserDetails
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController

@RestController
class ProfilesController: ProfilesApi{

    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var userService: UserService

    override fun profilesGet(): ResponseEntity<List<Profile?>?>? {
        TODO("Not yet implemented")
    }

    override fun profilesStringIdGet(stringId: @NotNull String): ResponseEntity<Profile?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, stringId!!)!!
        val p = Profile(profile.id, profile.stringId, profile.name)
        return ResponseEntity.ok(p)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ProfilesController::class.java)
    }
}
