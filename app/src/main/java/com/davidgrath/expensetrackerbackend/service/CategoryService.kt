package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.dtos.CategoryWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.GenericRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Mover
import com.davidgrath.expensetrackerbackend.utils.findEarliestOccupancySlot
import com.davidgrath.expensetrackerbackend.utils.getNextSortKey
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
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var genericRepository: GenericRepository
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

    fun getByProfileIdAndOrdinal(profileId: Long, categoryOrdinal: Short): CategoryPojo? {
        return categoryRepository.findByProfileIdAndOrdinal(profileId, categoryOrdinal)
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
        var retId = -1L
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val profile = profileRepository.getByIdForUpdateTrx(trxContext, profileId)
            LOGGER.info("Acquired lock for profile {}", profile.id)

            val existing = categoryRepository.getAllByProfileIdTrx(trxContext,profile.id!!)
            if(existing.size >= Constants.Constraints.MAX_CATEGORIES_PER_PROFILE) {
                val exception = CountLimitExceededException(Constants.Constraints.MAX_CATEGORIES_PER_PROFILE)
                LOGGER.error("Too many categories", exception)
                throw exception
            }
            val ordinalSortKeyOccupancySlot = categoryRepository.getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext, profileId)
            val nextOrdinal = ((ordinalSortKeyOccupancySlot.ordinal?: 0) + 1).toShort()

            val allSlots = categoryRepository.getAllOccupancySlotsTrx(trxContext, profileId)
//            val nextOccupancySlot = ((ordinalSortKeyOccupancySlot.occupancySlot?: 0) + 1).toShort()
            val nextOccupancySlot = findEarliestOccupancySlot(allSlots.toShortArray(), Constants.Constraints.MAX_CATEGORIES_PER_PROFILE)
            if(nextOccupancySlot == null) {
                throw CountLimitExceededException(Constants.Constraints.MAX_CATEGORIES_PER_PROFILE)
            }


            val idToSortKeyMap = existing.associate { it.id!! to it.sortKey!! }
            val mover = object: Mover<Long> {
                override fun moveSortKey(id: Long, sortKey: Short) {
                    categoryRepository.moveSortKeyTrx(trxContext, profileId, id, sortKey)
                }
            }

            val nextSortKey = getNextSortKey(ordinalSortKeyOccupancySlot.sortKey?:0, idToSortKeyMap, mover)

            val date = ZonedDateTime.now(timeHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
            val category = CategoryPojo(null, profileId, null, true.toByte(), name, icon, nextOrdinal, nextSortKey, nextOccupancySlot,utcDate)
            val id = categoryRepository.insertCategoryTrx(trxContext, category)
            LOGGER.info("ID: {}", id)
            retId = id
        }
        return retId
    }

    fun updateCategory(categoryId: Long, profileId: Long, name: String, icon: String): Int {
        val updatedCount = categoryRepository.updateCategory(categoryId, profileId, name, icon)
        return updatedCount
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CategoryService::class.java)
    }
}