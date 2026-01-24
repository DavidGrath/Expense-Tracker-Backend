package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.util.Locale

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
class UserServiceTest {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var itemRepository: TransactionItemRepository
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
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
    fun basicUserSignUpTest() {
        val locale = Locale("en", "GB")
        val userId = userService.createUser("user2", "password", locale)
        val profile = profileRepository.findByStringId(userId, Constants.DEFAULT_PROFILE_ID)
        assertNotNull(profile)
        //Assert default account exists
        val accounts = accountRepository.getAllByProfileId(profile!!.id!!)
        val account = accounts.firstOrNull()
        assertNotNull(account)
        assertEquals("GBP", account!!.currencyCode)
        val categories = categoryRepository.getAllByProfileId(profile.id!!)
        val defaultCategoriesSize = 17
        assertEquals(defaultCategoriesSize, categories.size)
    }
}