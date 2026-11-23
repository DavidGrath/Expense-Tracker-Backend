package com.davidgrath.expensetrackerbackend.utils

import java.io.File
import java.util.UUID

class UserImageFile(val userUuid: UUID, val profileUuid: UUID, folderMode: FolderMode, imageName: String) {
    enum class FolderMode {
        Draft,
        Data
    }
    val appRootScopedPath: String
    val userScopedPath: String

    init {

        require(!imageName.contains("/"))
        require(!imageName.contains(File.separatorChar))
        require(validateGenericSingleLineString(imageName))

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
            .append(Constants.SUBFOLDER_NAME_IMAGES).append("/")
            .append(imageName)
        appRootScopedPath = appRootBuilder.toString()

        val userScopedBuilder = StringBuilder()
        userScopedBuilder.append(Constants.FOLDER_NAME_PROFILES).append("/")
            .append(profileUuid.toString()).append("/")
            .append(mainFolder).append("/")
            .append(Constants.SUBFOLDER_NAME_IMAGES).append("/")
            .append(imageName)
        userScopedPath = userScopedBuilder.toString()
    }

}