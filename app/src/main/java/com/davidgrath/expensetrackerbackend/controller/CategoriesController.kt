package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.CategoriesApi
import com.davidgrath.expensetracker.generatedapi.model.Category
import com.davidgrath.expensetrackerbackend.service.CategoryService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.accountWithStatsToAccount
import com.davidgrath.expensetrackerbackend.utils.categoryPojoToCategory
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val categories = categoryService.getCategoriesForProfile(profile.id!!)
        val apiCategories = categories.map { c ->
            categoryPojoToCategory(c)
        }
        return ResponseEntity.ok(apiCategories)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CategoriesController::class.java)
    }
}