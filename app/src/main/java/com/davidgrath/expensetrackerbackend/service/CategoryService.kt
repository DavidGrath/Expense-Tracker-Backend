package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CategoryService {
    @Autowired
    lateinit var categoryRepository: CategoryRepository

    fun isCategoryForProfile(profileId: Long, categoryId: Long): Boolean {
        val category = categoryRepository.findByProfileIdAndId(profileId, categoryId)
        return category != null
    }

    fun getByProfileIdAndId(profileId: Long, categoryId: Long): CategoryPojo? {
        return categoryRepository.findByProfileIdAndId(profileId, categoryId)
    }
}