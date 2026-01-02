package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.api.SellersApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.Seller
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocation
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerUpdateRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SellersController: SellersApi {

    override fun sellersGet(profileId: @NotNull String): ResponseEntity<List<Seller?>?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<SellerDetails?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<List<SellerLocation?>?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsLocationIdGet(
        id: @NotNull Long,
        locationId: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<SellerLocation?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsLocationIdPut(
        id: @NotNull Long,
        locationId: @NotNull Long,
        profileId: @NotNull String,
        sellerLocationUpdateRequest: @Valid SellerLocationUpdateRequest?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsPost(
        id: @NotNull Long,
        profileId: @NotNull String,
        sellerLocationCreateRequest: @Valid SellerLocationCreateRequest?
    ): ResponseEntity<Long?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdPut(
        id: @NotNull Long,
        profileId: @NotNull String,
        sellerUpdateRequest: @Valid SellerUpdateRequest?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun sellersPost(
        profileId: @NotNull String,
        sellerCreateRequest: @Valid SellerCreateRequest?
    ): ResponseEntity<Long?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellersController::class.java)
    }
}