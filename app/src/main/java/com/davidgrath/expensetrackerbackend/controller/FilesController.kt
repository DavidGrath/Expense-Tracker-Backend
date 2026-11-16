package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.file
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class FilesController {

    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var userService: UserService

    @GetMapping("/files/profiles/{profileUuid}/{*path}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getFile(@PathVariable("profileUuid") profileUuid: String, @PathVariable("path") path: String): FileSystemResource? {
        val root = timeHandler.getRoot()

        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        //TODO Handle profile not exist

        val userUuidString = bytesToUUID(userUuid).toString()
        val file = file(root, Constants.FOLDER_NAME_USERS, userUuidString, Constants.FOLDER_NAME_PROFILES, profileUuid, path)
        LOGGER.debug("Absolute path: {}", file.absolutePath)
        if(!file.exists()) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Exception: ", exception)
            throw exception
        }


        return FileSystemResource(file)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FilesController::class.java)
    }
}