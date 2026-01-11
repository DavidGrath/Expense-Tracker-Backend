package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.api.DocumentsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.ImageDocumentStats
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
public class DocumentsController: DocumentsApi {

    override fun documentsGet(profileId: @NotNull String): ResponseEntity<List<DocumentDetails?>?>? {
        TODO("Not yet implemented")
    }

    override fun documentsIdGet(
        profileId: @NotNull String,
        id: @NotNull Long
    ): ResponseEntity<DocumentDetails?>? {
        TODO("Not yet implemented")
    }

    override fun documentsStatsGet(profileId: @NotNull String): ResponseEntity<ImageDocumentStats?>? {
        TODO("Not yet implemented")
    }
}
