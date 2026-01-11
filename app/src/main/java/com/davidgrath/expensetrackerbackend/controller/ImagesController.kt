package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.api.ImagesApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.ImageDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.ImageDocumentStats
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ImagesController: ImagesApi {
    override fun imagesGet(profileId: @NotNull String): ResponseEntity<List<ImageDetails?>?>? {
        TODO("Not yet implemented")
    }

    override fun imagesIdGet(
        profileId: @NotNull String,
        id: @NotNull Long
    ): ResponseEntity<ImageDetails?>? {
        TODO("Not yet implemented")
    }

    override fun imagesStatsGet(profileId: @NotNull String): ResponseEntity<ImageDocumentStats?>? {
        TODO("Not yet implemented")
    }
}