package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.views.CategoryWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class CategoryService {
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var timeHandler: TimeHandler

    fun isCategoryForProfile(profileId: Long, categoryId: Long): Boolean {
        val category = categoryRepository.findByProfileIdAndId(profileId, categoryId)
        val value = category != null
        LOGGER.info("Category for profile: {}", value)
        return value
    }

    fun isCategoryForUser(userId: Long, categoryId: Long): Boolean {
        val category = categoryRepository.findByUserIdAndId(userId, categoryId)
        val value = category != null
        LOGGER.info("Category for user: {}", value)
        return value
    }

    fun getByProfileIdAndId(profileId: Long, categoryId: Long): CategoryPojo? {
        return categoryRepository.findByProfileIdAndId(profileId, categoryId)
    }

    fun getCategoriesForProfile(profileId: Long): List<CategoryPojo> {
        val categories = categoryRepository.getAllByProfileId(profileId)
        LOGGER.info("Categories size: {}", categories.size)
        return categories
    }

    fun getCategoryDetailsForProfile(profileId: Long): List<CategoryWithStats> {
        val categories = categoryRepository.getAllByProfileIdWithStats(profileId)
        LOGGER.info("Categories size: {}", categories.size)
        return categories
    }

    fun createCategory(profileId: Long, name: String, icon: String): Long {
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val category = CategoryPojo(null, profileId, null, true.toByte(), name, utcDate, icon)
        return categoryRepository.insertCategory(category)
    }

    fun updateCategory(categoryId: Long, profileId: Long, name: String, icon: String): Int {
        val updatedCount = categoryRepository.updateCategory(categoryId, profileId, name, icon)
        return updatedCount
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CategoryService::class.java)
    }
}