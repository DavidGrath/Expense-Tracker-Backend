package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.api.SellersApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.Seller
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocation
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerUpdateRequest
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.SellerService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.Constants
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController

@RestController
class SellersController: SellersApi {

    @Autowired
    lateinit var sellerService: SellerService
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService

    override fun sellersGet(profileId: @NotNull String): ResponseEntity<List<Seller?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val sellers = sellerService.getAll(profile.id!!)
        val apiSellers = sellers.map { s ->
            Seller(s.id, s.name)
        }
        return ResponseEntity.ok(apiSellers)
    }

    override fun sellersIdGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<SellerDetails?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        if(!sellerService.isSellerForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not for user", exception)
            throw exception
        }
        if(!sellerService.isSellerInProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Seller not for profile", exception)
            throw exception
        }
        val seller = sellerService.getByProfileIdAndId(profile.id!!, id)

        if(seller == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not found", exception)
            throw exception
        }
        val locations = sellerService.getAllSellerLocations(profile.id!!, id)
        val apiLocations = locations.map { l ->
            SellerLocation(l.id, l.location)
        }
        val details = SellerDetails(seller.id, seller.name, apiLocations)
        return ResponseEntity.ok(details)
    }

    override fun sellersIdLocationsGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<List<SellerLocation?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        if(!sellerService.isSellerForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not for user", exception)
            throw exception
        }
        if(!sellerService.isSellerInProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Seller not for profile", exception)
            throw exception
        }

        val seller = sellerService.getByProfileIdAndId(profile.id!!, id)

        if(seller == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not found", exception)
            throw exception
        }
        val locations = sellerService.getAllSellerLocations(profile.id!!, id)
        val apiLocations = locations.map { l ->
            SellerLocation(l.id, l.location)
        }

        return ResponseEntity.ok(apiLocations)
    }

    override fun sellersIdLocationsLocationIdGet(
        id: @NotNull Long,
        locationId: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<SellerLocation?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        if(!sellerService.isSellerForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not for user", exception)
            throw exception
        }
        if(!sellerService.isSellerInProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Seller not for profile", exception)
            throw exception
        }

        val seller = sellerService.getByProfileIdAndId(profile.id!!, id)

        if(seller == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not found", exception)
            throw exception
        }


        // Location

        if(!sellerService.isLocationForUser(user.id!!, locationId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Location not for user", exception)
            throw exception
        }
        if(!sellerService.isLocationInProfile(profile.id!!, locationId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Location not for profile", exception)
            throw exception
        }
        val location = sellerService.getSellerLocation(profile.id!!, locationId)

        if(location == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Location not found", exception)
            throw exception
        }
        val apiLocation = SellerLocation(location.id, location.location)

        return ResponseEntity.ok(apiLocation)
    }

    override fun sellersIdLocationsLocationIdPut(
        id: @NotNull Long,
        locationId: @NotNull Long,
        profileId: @NotNull String,
        sellerLocationUpdateRequest: @Valid SellerLocationUpdateRequest?
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        if(!sellerService.isSellerForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not for user", exception)
            throw exception
        }
        if(!sellerService.isSellerInProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Seller not for profile", exception)
            throw exception
        }

        val seller = sellerService.getByProfileIdAndId(profile.id!!, id)

        if(seller == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not found", exception)
            throw exception
        }

        if(!sellerService.isLocationForUser(user.id!!, locationId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Location not for user", exception)
            throw exception
        }
        if(!sellerService.isLocationInProfile(profile.id!!, locationId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Location not for profile", exception)
            throw exception
        }
        val location = sellerService.getSellerLocation(profile.id!!, locationId)

        if(location == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Location not found", exception)
            throw exception
        }

        val nameTrimmed = sellerLocationUpdateRequest!!.name!!.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val result = sellerService.updateSellerLocation(profile.id, id, nameTrimmed)
        return ResponseEntity.ok().build()
    }

    override fun sellersIdLocationsPost(
        id: @NotNull Long,
        profileId: @NotNull String,
        sellerLocationCreateRequest: @Valid SellerLocationCreateRequest?
    ): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!


        if(!sellerService.isSellerForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not for user", exception)
            throw exception
        }
        if(!sellerService.isSellerInProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Seller not for profile", exception)
            throw exception
        }
        val seller = sellerService.getByProfileIdAndId(profile.id!!, id)

        if(seller == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not found", exception)
            throw exception
        }

        val locationCount = sellerService.countAllLocations(profile.id!!, id)
        if(locationCount >= Constants.Constraints.MAX_LOCATIONS_PER_SELLER) {
            val exception = CustomHttpException(ErrorCode.LIMITS_EXCEEDED, HttpStatus.BAD_REQUEST)
            LOGGER.error("Max locations reached", exception)
            throw exception
        }


        val nameTrimmed = sellerLocationCreateRequest!!.name.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val locationId = sellerService.createSellerLocation(id, nameTrimmed)
        return ResponseEntity.ok(locationId)
    }

    override fun sellersIdPut(
        id: @NotNull Long,
        profileId: @NotNull String,
        sellerUpdateRequest: @Valid SellerUpdateRequest?
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!


        if(!sellerService.isSellerForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not for user", exception)
            throw exception
        }
        if(!sellerService.isSellerInProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Seller not for profile", exception)
            throw exception
        }

        val seller = sellerService.getByProfileIdAndId(profile.id!!, id)

        if(seller == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Seller not found", exception)
            throw exception
        }
        val nameTrimmed = sellerUpdateRequest!!.name!!.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val result = sellerService.updateSeller(profile.id, id, nameTrimmed)
        return ResponseEntity.ok().build()
    }

    override fun sellersPost(
        profileId: @NotNull String,
        sellerCreateRequest: @Valid SellerCreateRequest?
    ): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val sellerCount = sellerService.countAllSellers(profile.id!!)
        if(sellerCount >= Constants.Constraints.MAX_SELLERS_PER_PROFILE) {
            val exception = CustomHttpException(ErrorCode.LIMITS_EXCEEDED, HttpStatus.BAD_REQUEST)
            LOGGER.error("Max sellers reached", exception)
            throw exception
        }
        val nameTrimmed = sellerCreateRequest!!.name.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val sellerId = sellerService.createSeller(profile.id, nameTrimmed)
        return ResponseEntity.ok(sellerId)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellersController::class.java)
    }
}