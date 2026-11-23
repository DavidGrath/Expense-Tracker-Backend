package com.davidgrath.expensetrackerbackend.utils

import java.io.File
import java.time.LocalDate
import java.util.*

class UserDocumentFile(val userUuid: UUID, val profileUuid: UUID, folderMode: FolderMode, date: LocalDate?, documentName: String) {

    enum class FolderMode {
        Draft,
        Data
    }
    val appRootScopedPath: String
    val userScopedPath: String

    init {

        require(!documentName.contains("/"))
        require(!documentName.contains(File.separatorChar))
        require(validateGenericSingleLineString(documentName))
        if(folderMode == FolderMode.Data) {
            require(date != null)
        }

        val mainFolder = if(folderMode == FolderMode.Draft) {
            Constants.FOLDER_NAME_DRAFT
        } else {
            Constants.FOLDER_NAME_DATA
        }


        val appRootBuilder = StringBuilder()
        appRootBuilder.append(Constants.FOLDER_NAME_USERS).append("/")
            .append(userUuid.toString()).append("/")
            .append(Constants.FOLDER_NAME_PROFILES).append("/")
            .append(profileUuid.toString()).append("/")
            .append(mainFolder).append("/")
            .append(Constants.SUBFOLDER_NAME_DOCUMENTS).append("/")

        //Don't bother creating a y/m/d structure for a document in draft
        if(folderMode == FolderMode.Data) {
            val yearMonthDay = date.toString().split("-") //Used this instead of getting the integer fields So that we can get the zero-padding early
            val year = yearMonthDay[0]
            val month = yearMonthDay[1]
            val day = yearMonthDay[2]
            appRootBuilder
                .append(year).append("/")
                .append(month).append("/")
                .append(day).append("/")
        }
            appRootBuilder
            .append(documentName)
        appRootScopedPath = appRootBuilder.toString()

        val userScopedBuilder = StringBuilder()
        userScopedBuilder.append(Constants.FOLDER_NAME_PROFILES).append("/")
            .append(profileUuid.toString()).append("/")
            .append(mainFolder).append("/")
            .append(Constants.SUBFOLDER_NAME_DOCUMENTS).append("/")

        if(folderMode == FolderMode.Data) {
            val yearMonthDay = date.toString().split("-")
            val year = yearMonthDay[0]
            val month = yearMonthDay[1]
            val day = yearMonthDay[2]
            userScopedBuilder
                .append(year).append("/")
                .append(month).append("/")
                .append(day).append("/")
        }
        userScopedBuilder
            .append(documentName)
        userScopedPath = userScopedBuilder.toString()
    }

}


