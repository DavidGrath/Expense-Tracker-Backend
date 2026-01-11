package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.file
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDate

@Service
class FileService {

    @Autowired
    lateinit var timeHandler: TimeHandler


    fun deleteDraftFiles(root: File, userUuid: String, profileStringId: String) {

        val documents = file(root,
            Constants.FOLDER_NAME_USERS, userUuid,
            Constants.FOLDER_NAME_PROFILES, profileStringId,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        if(documents.exists()) {
            val children = documents.listFiles()
            val size = children.size
            if(size > 0) {
                documents.deleteRecursively()
                LOGGER.info("Deleted {} unused files in {}", size, documents)
            } else {
                LOGGER.info("Deleted empty directory {}", documents)
            }
        }
        val images = file(root,
            Constants.FOLDER_NAME_USERS, userUuid,
            Constants.FOLDER_NAME_PROFILES, profileStringId,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        if(images.exists()) {
            val children = images.listFiles()
            val size = children.size
            if(size > 0) {
                images.deleteRecursively()
                LOGGER.info("Deleted {} unused files in {}", size, images)
            } else {
                images.delete()
                LOGGER.info("Deleted empty directory {}", images)
            }
        }
    }

    fun moveFileToMain(root: File, userUuid: String, profileStringId: String, sourceFile: File, subfolder: String, date: LocalDate?): String {
        var path: String? = null
        var destination: File? = null
        if(subfolder == Constants.SUBFOLDER_NAME_DOCUMENTS) {
            val year = String.format("%04d", date!!.year)
            val month = String.format("%02d", date!!.monthValue)
            val day = String.format("%02d", date!!.dayOfMonth)

            path = "/profiles/${profileStringId}/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_DOCUMENTS}/${year}/${month}/${day}/${sourceFile.name}"
            destination = file(root,
                Constants.FOLDER_NAME_USERS, userUuid,
                Constants.FOLDER_NAME_PROFILES, profileStringId,
                Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, year, month, day, sourceFile.name)
        } else if (subfolder == Constants.SUBFOLDER_NAME_IMAGES) {
            path = "/profiles/${profileStringId}/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_IMAGES}/${sourceFile.name}"
            destination = file(root,
                Constants.FOLDER_NAME_USERS, userUuid,
                Constants.FOLDER_NAME_PROFILES, profileStringId,
                Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES, sourceFile.name)
        }
            if(destination!!.exists()) {
                LOGGER.debug("Destination {} exists", destination.path)
                return path!!
            }
        sourceFile.copyTo(destination)
        LOGGER.info("Created {}", destination.path)

        val b = sourceFile.delete()
        LOGGER.info("Deleted {}: {}", sourceFile.path, b)
        return path!!
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FileService::class.java)
    }
}