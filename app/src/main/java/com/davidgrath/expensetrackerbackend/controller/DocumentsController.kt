package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.api.DocumentsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.service.DocumentService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.documentPojoToDocumentDetails
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
public class DocumentsController: DocumentsApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var documentService: DocumentService

    @Autowired
    lateinit var httpServletRequest: HttpServletRequest

    override fun documentsGet(profileId: @NotNull String): ResponseEntity<List<DocumentDetails?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        val documents = documentService.getAllUserDocuments(profile.id!!)
        val apiDocuments = documents.map { d ->
            documentPojoToDocumentDetails(d, triple)
        }
        return ResponseEntity.ok(apiDocuments)
    }

    override fun documentsIdGet(
        profileId: @NotNull String,
        id: @NotNull Long
    ): ResponseEntity<DocumentDetails?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        if(!documentService.isDocumentForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Document not for user", exception)
            throw exception
        }
        if(!documentService.isDocumentForProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Document not for profile", exception)
            throw exception
        }

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        val document = documentService.getByProfileIdAndId(profile.id!!, id)

        if(document == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Document not found", exception)
            throw exception
        }
        val apiDocument = documentPojoToDocumentDetails(document, triple)

        return ResponseEntity.ok(apiDocument)
    }

    override fun documentsStatsGet(profileId: @NotNull String): ResponseEntity<ImageDocumentStats?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val stats = documentService.getDocumentStats(profile.id!!)
        val countAndSize = ImageDocumentStats(stats.count, stats.totalSizeBytes)

        return ResponseEntity.ok(countAndSize)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DocumentsController::class.java)
    }
}
