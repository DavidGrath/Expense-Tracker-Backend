package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.api.CategoriesApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.Category
import com.davidgrath.expensetrackerbackend.generatedapi.model.CategoryCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.CategoryDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.CategoryUpdateRequest
import com.davidgrath.expensetrackerbackend.service.CategoryService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController

@RestController
class CategoriesController: CategoriesApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var categoryService: CategoryService

    override fun categoriesGet(profileId: @NotNull String): ResponseEntity<List<Category?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!

        val categories = categoryService.getCategoriesForProfile(profile.id!!)
        val apiCategories = categories.map { c ->
            categoryPojoToCategory(c)
        }
        return ResponseEntity.ok(apiCategories)
    }

    override fun categoriesIdPut(id: @NotNull Long, profileId: @NotNull String, categoryUpdateRequest: @Valid CategoryUpdateRequest?): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        if(!categoryService.isCategoryForUser(user.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Category not for user", exception)
            throw exception
        }
        if(!categoryService.isCategoryForProfile(profile.id!!, id)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Category not for profile", exception)
            throw exception
        }
        val category = categoryService.getByProfileIdAndId(profile.id!!, id)!!
        if(!category.isCustom.toBoolean()) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_NOT_MODIFIABLE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Category is default", exception)
            throw exception
        }
        val nameTrimmed = categoryUpdateRequest!!.name.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val icon = categoryUpdateRequest!!.icon
        val namespacedIcon = icon.split(':')
        if(namespacedIcon.isEmpty()) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_ICON_FORMAT_INCORRECT, HttpStatus.BAD_REQUEST)
            LOGGER.error("No icon", exception)
            throw exception
        }
        if(!namespacedIcon.first().equals(Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS, true)) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_ICON_FORMAT_INCORRECT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Icon format not correct", exception)
            throw exception
        }
        val loader = this.javaClass.classLoader!!
        val legalIconNames = loadMaterialSymbolsIcons(loader)
        if(namespacedIcon[1].lowercase() !in legalIconNames) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_ICON_NAME_INCORRECT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Icon name not correct", exception)
            throw exception
        }
        val rows = categoryService.updateCategory(id, profile.id!!, nameTrimmed, categoryUpdateRequest.icon)

        return ResponseEntity.ok().build()
    }

    override fun categoriesPost(profileId: @NotNull String, categoryCreateRequest: @Valid CategoryCreateRequest?): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!

        val existing = categoryService.getCategoriesForProfile(profile.id!!) //TODO TOC
        if(existing.size >= Constants.Constraints.MAX_CATEGORIES_PER_PROFILE) {
            val exception = CustomHttpException(ErrorCode.LIMITS_EXCEEDED, HttpStatus.BAD_REQUEST)
            LOGGER.error("Too many categories", exception)
            throw exception
        }
        val nameTrimmed = categoryCreateRequest!!.name.trim()
        if(nameTrimmed.isBlank()) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Name blank", exception)
            throw exception
        }
        val icon = categoryCreateRequest!!.icon
        val namespacedIcon = icon.split(':')
        if(namespacedIcon.isEmpty()) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_ICON_FORMAT_INCORRECT, HttpStatus.BAD_REQUEST)
            LOGGER.error("No icon", exception)
            throw exception
        }
        if(!namespacedIcon.first().equals(Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS, true)) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_ICON_FORMAT_INCORRECT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Icon format not correct", exception)
            throw exception
        }
        val loader = this.javaClass.classLoader!!
        val legalIconNames = loadMaterialSymbolsIcons(loader)
        if(namespacedIcon[1].lowercase() !in legalIconNames) {
            val exception = CustomHttpException(ErrorCode.CATEGORY_ICON_NAME_INCORRECT, HttpStatus.BAD_REQUEST)
            LOGGER.error("Icon name not correct", exception)
            throw exception
        }
        val categoryId = categoryService.createCategory(profile.id!!, categoryCreateRequest.name, categoryCreateRequest.icon) //TODO TOU
        //TODO TOCTOU
        return ResponseEntity.ok(categoryId)
    }

    override fun categoriesDetailsGet(profileId: @NotNull String): ResponseEntity<List<CategoryDetails?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!

        val categories = categoryService.getCategoryDetailsForProfile(profile.id!!)
        val apiCategories = categories.map { c ->
            categoryWithStatsToApiCategory(c)
        }
        return ResponseEntity.ok(apiCategories)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CategoriesController::class.java)
    }
}