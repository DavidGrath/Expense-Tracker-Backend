package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.SellersApi
import com.davidgrath.expensetracker.generatedapi.model.Seller
import com.davidgrath.expensetracker.generatedapi.model.SellerCreateRequest
import com.davidgrath.expensetracker.generatedapi.model.SellerDetails
import com.davidgrath.expensetracker.generatedapi.model.SellerLocation
import com.davidgrath.expensetracker.generatedapi.model.SellerLocationCreateRequest
import com.davidgrath.expensetracker.generatedapi.model.SellerLocationUpdateRequest
import com.davidgrath.expensetracker.generatedapi.model.SellerUpdateRequest
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

    override fun sellersIdGet(profileId: @NotNull String): ResponseEntity<SellerDetails?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsGet(profileId: @NotNull String): ResponseEntity<List<SellerLocation?>?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsIdGet(profileId: @NotNull String): ResponseEntity<SellerLocation?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsIdPut(
        profileId: @NotNull String,
        sellerLocationUpdateRequest: @Valid SellerLocationUpdateRequest?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdLocationsPost(
        profileId: @NotNull String,
        sellerLocationCreateRequest: @Valid SellerLocationCreateRequest?
    ): ResponseEntity<Long?>? {
        TODO("Not yet implemented")
    }

    override fun sellersIdPut(
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