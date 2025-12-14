package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.CustomUserDetails
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.file
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File

@RestController
class FilesController {

    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var userService: UserService

    @GetMapping("/files/profiles/{profileUuid}/{*path}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getFile(@PathVariable("profileUuid") profileUuid: String, @PathVariable("path") path: String): FileSystemResource? {
        val map = LinkedMultiValueMap<String, Any>()
        val root = timeHandler.getRoot()

        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as CustomUserDetails
        LOGGER.info("Username: {}", principal.username)
        val user = userService.getByUsername(principal.username!!)
        val userUuid = user!!.uuid!!

        val file = file(root, Constants.FOLDER_NAME_USERS, userUuid, Constants.FOLDER_NAME_PROFILES, profileUuid, path)
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