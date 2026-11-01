package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.api.ImagesApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.ImageDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.service.ImageService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.imageWithStatsToImageDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController

@RestController
class ImagesController: ImagesApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var imageService: ImageService

    @Autowired
    lateinit var httpServletRequest: HttpServletRequest

    override fun imagesGet(profileId: @NotNull String): ResponseEntity<List<ImageDetails?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        val images = imageService.getAllImages(profile.id!!)
        val apiImages = images.map { i ->
            imageWithStatsToImageDetails(i, triple)
        }
        return ResponseEntity.ok(apiImages)
    }

    override fun imagesIdGet(
        profileId: @NotNull String,
        id: @NotNull Long
    ): ResponseEntity<ImageDetails?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!

        if(!imageService.isImageForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Image not for user", exception)
            throw exception
        }
        if(!imageService.isImageForProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Image not for profile", exception)
            throw exception
        }

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        val image = imageService.getByProfileIdAndId(profile.id!!, id)

        if(image == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Image not found", exception)
            throw exception
        }
        val apiImage = imageWithStatsToImageDetails(image, triple)

        return ResponseEntity.ok(apiImage)
    }

    override fun imagesStatsGet(profileId: @NotNull String): ResponseEntity<ImageDocumentStats?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!

        val stats = imageService.getImageStats(profile.id!!)
        val countAndSize = ImageDocumentStats(stats.count, stats.totalSizeBytes)

        return ResponseEntity.ok(countAndSize)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImagesController::class.java)
    }

}