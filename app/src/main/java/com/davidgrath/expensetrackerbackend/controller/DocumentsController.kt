package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.DocumentsApi
import com.davidgrath.expensetracker.generatedapi.model.DocumentDetails
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
}
