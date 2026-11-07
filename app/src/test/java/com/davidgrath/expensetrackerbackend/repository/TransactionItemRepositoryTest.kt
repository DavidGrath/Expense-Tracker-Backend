package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.SearchFilterDb
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionRepository::class,
        ProfileRepository::class,
        UserRepository::class,
        AccountRepository::class
    ])
class TransactionItemRepositoryTest {

    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository

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

    /**
     * We get the strings raw from the database and then filter them at the application layer, so we're
     * not testing for search term matches here
     */
    @Test
    fun getTransactionItemsLimitFilterTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()



        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)


        val fitness = categoryRepository.findByProfileIdAndStringId(profile!!.id!!, "fitness")!!
        val food = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")!!
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")!!
        val transport = categoryRepository.findByProfileIdAndStringId(profile.id!!, "transportation")!!

        val firstDate = LocalDate.parse("2025-01-01") // Wednesday
        val secondDate = LocalDate.parse("2025-01-02")
        val thirdDate = LocalDate.parse("2025-01-03")
        val forthDate = LocalDate.parse("2025-01-04")
        val fifthDate = LocalDate.parse("2025-01-05")
        val sixthDate = LocalDate.parse("2025-01-06")
        val seventhDate = LocalDate.parse("2025-01-07")
        val eighthDate = LocalDate.parse("2025-01-08")

        val transactionBuilder = dataBuilder.createTransaction()
            .atDate(firstDate)
            .withItem("Bread", "food", BigDecimal(1_500))
            .withItem("Dumbbells", "fitness", BigDecimal(2_500))
            .withNewAccount("GBP", "Test Account")
        val poundsAccountId = transactionBuilder.getAccount().id!!
        val id = transactionBuilder.commit().first()

        val id2 = dataBuilder.createTransaction()
            .atDate(secondDate)
            .withItem("Hammer", "utilities", BigDecimal(5_000))
            .withItem("Sweatpants", "fitness", BigDecimal(5_000))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(thirdDate)
            .withItem("Fish", "food", BigDecimal(3_000))
            .withItem("Red shirt", "clothing", BigDecimal(5_000))
            .withItem("Fuel", "transportation", BigDecimal(5_000))
            .commit()

        dataBuilder.createTransaction()
            .atDate(forthDate)
            .withItem("Medicine", "healthcare", BigDecimal(1_000))
            .withNewSeller("Generic Hospital")
            .withNewAccount("USD", "Test Account 2")
            .commit()

        val firstSellerId = sellerRepository.getAll(profile.id!!).first()!!.id!!

        dataBuilder.createTransaction()
            .atDate(fifthDate)
            .withItem("Vitamin C", "healthcare", BigDecimal(1_000))
            .withSeller(firstSellerId)
            .withNewSellerLocation("Brand New Branch")
            .withNewAccount("USD", "Test Account 3")
            .commit()

        dataBuilder.createTransaction()
            .atDate(sixthDate)
            .withItem("Bandages", "healthcare", BigDecimal(1_000))
            .withSeller(firstSellerId)
            .withNewSellerLocation("New New Branch")
            .withNewAccount("USD", "Test Account 4")
            .commit()

        dataBuilder.createTransaction()
            .atDate(seventhDate)
            .withItem("Light bulb", "household", BigDecimal(1_000))
            .withNewSeller("Generic Hardware Store")
            .withNewAccount("USD", "Test Account 5")
            .withMode(TransactionMode.Card)
            .commit()


        val secondSellerId = sellerRepository.getAll(profile.id!!)[1].id!!

        dataBuilder.createTransaction()
            .atDate(eighthDate)
            .withItem("Paint", "household", BigDecimal(1_000))
            .withItem("Light bulb 13W", "household", BigDecimal(1_000))
            .withSeller(secondSellerId)
            .withNewSellerLocation("Somewhere out there")
            .withNewAccount("CAD", "Test Account 6")
            .withMode(TransactionMode.Card)
            .commit()

        var searchFilter = SearchFilterDb()

        val itemsNoFilter = transactionItemRepository.getAllItemsLimit(profile!!.id!!, searchFilter).map { it.description }



        val categories = listOf(food, clothing, transport).map { it.id!! }
        searchFilter = SearchFilterDb(categoryIds = categories)
        val itemsFilteredByCategory = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(currencyCodes = listOf(Currency.getInstance("USD")))
        val itemsFilteredByCurrencyCodes = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(currencyCodes = listOf(Currency.getInstance("EUR")))
        val itemsFilteredByCurrencyCodesNotOwnedByUser = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(currencyCodes = listOf(Currency.getInstance("USD")), accountIds = listOf(accountId, poundsAccountId))
        val itemsFilteredByCurrencyCodesWithOverlappingAccounts = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(accountIds = listOf(accountId, poundsAccountId))
        val itemsFilteredByAccount = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(weekDays = listOf(DayOfWeek.WEDNESDAY))
        val itemsFilteredByWeekday = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(transactionModes = listOf(TransactionMode.Card))
        val itemsFilteredByMode = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        val firstSellerFirstLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId).first().id!!
        val firstSellerSecondLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId)[1].id!!
        val secondSellerLocationId =  sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, secondSellerId).first().id!!

        searchFilter = SearchFilterDb(sellerIds = listOf(firstSellerId))
        val itemsFilteredBySellers = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(sellerIds = listOf(firstSellerId), sellerLocationIds = listOf(secondSellerLocationId))
        val itemsFilteredBySellersWithOverlappingLocationIds = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        searchFilter = SearchFilterDb(sellerLocationIds = listOf(firstSellerFirstLocationId, secondSellerLocationId))
        val itemsFilteredByLocation = transactionItemRepository.getAllItemsLimit(profile.id!!, searchFilter).map { it.description }

        assertThat(itemsNoFilter, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByCategory, Matchers.containsInAnyOrder("Bread","Fish","Red shirt","Fuel"))
        assertThat(itemsFilteredByWeekday, Matchers.containsInAnyOrder("Bread","Dumbbells","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByCurrencyCodes, Matchers.containsInAnyOrder("Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb"))
        assertThat(itemsFilteredByAccount, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel"))
        assertThat(itemsFilteredByMode, Matchers.containsInAnyOrder("Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredBySellers, Matchers.containsInAnyOrder("Medicine","Vitamin C","Bandages"))
        assertThat(itemsFilteredByLocation, Matchers.containsInAnyOrder("Vitamin C","Paint","Light bulb 13W"))

        //Special cases from documentation. I could make them a separate test, but leave them here for now
        assertThat(itemsFilteredByCurrencyCodesWithOverlappingAccounts, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb"))
        assertThat(itemsFilteredBySellersWithOverlappingLocationIds, Matchers.containsInAnyOrder("Medicine","Vitamin C","Bandages","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByCurrencyCodesNotOwnedByUser, `is`(empty()))
    }

}