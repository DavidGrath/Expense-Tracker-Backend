package com.davidgrath.expensetrackerbackend.controller;

import com.davidgrath.expensetrackerbackend.generatedapi.api.ProfilesApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.Profile
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.doTheOtelThing
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
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profilePojos = profileService.getAllByUserId(user!!.id!!)!!
        val profiles = profilePojos.map {
            Profile(it.id, bytesToUUID(it.uuid!!).toString(), it.name)
        }
        return ResponseEntity.ok(profiles)
    }

    override fun profilesUuidGet(uuid: @NotNull String): ResponseEntity<Profile?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        doTheOtelThing(user!!.uuid!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, uuid!!)!!
        val p = Profile(profile.id, bytesToUUID(profile.uuid!!).toString(), profile.name)
        return ResponseEntity.ok(p)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ProfilesController::class.java)
    }
}
