package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.api.UsersApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.UserDetails
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.doTheOtelThing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset

@RestController
class UsersController: UsersApi {

    @Autowired
    lateinit var userService: UserService

    override fun usersMeGet(): ResponseEntity<UserDetails?>? {


        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        doTheOtelThing(user!!.uuid!!)

        val userDetails = UserDetails(null, null, bytesToUUID(user!!.uuid!!), user.username, null, user.createdAt!!.atOffset(ZoneOffset.UTC)) //TODO rework user signup flow
        return ResponseEntity.ok(userDetails)
    }
}