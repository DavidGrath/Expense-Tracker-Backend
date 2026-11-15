package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
class CategoryServiceTest {

    @Autowired
    lateinit var categoryService: CategoryService
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test
    fun givenCategoriesFullWhenCreateCategoryThenLimitExceeded() {


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val existing = categoryRepository.getAllByProfileId(profile.id!!)
        val remaining = Constants.Constraints.MAX_CATEGORIES_PER_PROFILE - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing sellers too many")
        }
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        for(sel in 0 until remaining) {
            val ordinal = (sel+1+existing.size).toShort()
            val sortKey = ((sel+1+existing.size) * 1_000).toShort()
            val occupancySlot = (sel+1+existing.size).toShort()
            val sellerLocation = CategoryPojo(null, profile.id!!, null, TRUE_BYTE, "Custom",
                null, ordinal, sortKey, occupancySlot, LocalDateTime.now(timeHandler.getClock()))
            categoryRepository.insertCategory(sellerLocation)
        }
        Assert.assertThrows(CountLimitExceededException::class.java) {
            categoryService.createCategory(profile.id!!, "New", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        }
    }


    @Test
    fun givenCategorySortKeysFullWhenCreateCategoryThenRebalanced() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val existing = categoryRepository.getAllByProfileId(profile.id!!)
        val remaining = Constants.Constraints.MAX_CATEGORIES_PER_PROFILE - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing sellers too many")
        }
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        for(sel in 0 until remaining-1) {
            val ordinal = (sel+1+existing.size).toShort()
            val sortKey = if(sel == remaining-2) {
                32766
            } else {
                ((sel+1+existing.size) * 1_100).toShort()
            }
            val occupancySlot = (sel+1+existing.size).toShort()
            val sellerLocation = CategoryPojo(null, profile.id!!, null, TRUE_BYTE, "Custom",
                null, ordinal, sortKey, occupancySlot, LocalDateTime.now(timeHandler.getClock()))
            categoryRepository.insertCategory(sellerLocation)
        }
        categoryService.createCategory(profile.id!!, "New", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val sequence = (1_000..30_000).step(1_000).toList().map { it.toShort() }
        val sortKeys = categoryRepository.getAllByProfileId(profile.id!!).map { it.sortKey!! }.sorted()
        assertEquals(sequence, sortKeys)
    }
}