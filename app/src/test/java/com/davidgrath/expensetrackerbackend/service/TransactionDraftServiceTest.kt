package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.TransactionDraftItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.assertEqualsBD
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.toBoolean
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
class TransactionDraftServiceTest {

    @Autowired
    lateinit var transactionDraftService: TransactionDraftService
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
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
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
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
    fun givenItemIsReductionWhenSaveThenRelevantFieldsPreservedAndOthersDiscarded() {

        //Add item with isReduction
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)
        transactionDraftService.addItemToDraft(profileId)

        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal(11), description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        //Add item with isReduction
        val secondItem = draft!!.items[1]
        val modified = secondItem.copy(isReduction = true, amount = BigDecimal.TEN,description = "Discount", quantity = 13, brand = "Generic", variation = "XXL", referenceNumber = "REF-1234")
        transactionDraftService.modifyDraftItem(profileId, secondItem.uuid!!, modified)
        //Fill in all fields
        transactionDraftService.finishDraft(profileId, ZoneId.of("UTC"))
        val account = accountRepository.getAllByProfileId(profileId).first()
        val transactions = transactionRepository.getTransactions(profileId, null, null, listOf(account.id!!), emptyList(), emptyList(), emptyList(), emptyList())
        val transaction = transactions.first()
        val item = transactionItemRepository.getAllByTransactionId(profileId, transaction.id!!)[1]

        assertEqualsBD(BigDecimal.ONE, transaction.amount!!)
        //Assert amount, description, and categoryId remain
        assertTrue(item.isReduction.toBoolean())
        assertEqualsBD(BigDecimal.TEN, item.amount!!)
        assertEquals("Discount", item.description)
        assertEquals(misc.id!!, item.primaryCategoryId)
        //Assert all other fields discarded
        assertNull(item.variation)
        assertNull(item.brand)
        assertNull(item.referenceNumber)
        assertEquals(1, item.quantity)

    }

    @Test
    fun givenTransactionHasReductionsAndReductionsLessThanTotalThenTransactionIsValid() {
        //Add normal item


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        transactionDraftService.addItemToDraft(profileId)
        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal(11), description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)


        //Add item with isReduction
        val secondItem = draft!!.items[1]
        val modified = secondItem.copy(amount = BigDecimal.TEN, description = "Discount", isReduction = true)
        transactionDraftService.modifyDraftItem(profileId, secondItem.uuid!!, modified)

        val isValid = transactionDraftService.validateDraft(profileId)
        assertTrue(isValid)
    }

    @Test
    fun givenTransactionHasReductionsAndReductionsNotLessThanTotalThenTransactionIsNotValid() {
        //Add normal item

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)
        transactionDraftService.addItemToDraft(profileId)
        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val secondItem = draft!!.items[1]
        val modifiedSecond = secondItem.copy(amount = BigDecimal.TEN, description = "Discount", isReduction = true)
        transactionDraftService.modifyDraftItem(profileId, secondItem.uuid!!, modifiedSecond)

        //Add item with isReduction
        val isValid = transactionDraftService.validateDraft(profileId)
        assertFalse(isValid)
    }

    @Test
    fun givenTransactionHasReductionsAndTransactionHasNoNormalItemsThenFail() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        //Change item to isReduction
        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Discount", isReduction = true)
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)
        //No other items
        //Fail - cannot have only reductions
        val isValid = transactionDraftService.validateDraft(profileId)
        assertFalse(isValid)
        //Oh, also, in general, negatives aren't allowed. Add multiple levels of assertion
    }

    @Test
    fun givenDateIsTodayBasedOnUserTimezoneAndTimeNotSetWhenSaveThenTimeIsNow() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = MOCKED_DATE
        transactionDraftService.setDate(profileId, localDate)


        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN)
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val transactionId = transactionDraftService.finishDraft(profileId, ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)!!
        val transactionTime = transaction.time!!

        assertEquals(0, MOCKED_TIME.compareTo(transactionTime))
    }

    @Test
    fun givenDateIsTodayBasedOnUserTimezoneAndTimeIsSetWhenSaveThenTimeIsCustom() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = MOCKED_DATE
        val localTime = LocalTime.of(1, 0, 30)
        transactionDraftService.setDate(profileId, localDate)
        transactionDraftService.setTime(profileId, localTime)

        val transactionId = transactionDraftService.finishDraft(profileId, ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)
        val transactionTime = transaction!!.time

        assertEquals(localTime, transactionTime)
    }

    @Test
    fun givenDateIsTodayBasedOnUserTimezoneAndDateIsNotTodayBasedOnServerTimezoneAndCustomDateIsNullAndTimeNotSetWhenSaveThenTimeIsNotNull() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = MOCKED_DATE
        val localTime = LocalTime.of(1, 0, 30)

        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val transactionId = transactionDraftService.finishDraft(profileId, ZoneId.of("Etc/GMT+10"))  // 10 PM 2025-06-29
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)
        val transactionTime = transaction!!.time




        assertNotNull(transaction.time)

    }

    @Test
    fun givenCustomDateIsBeforeTodayBasedOnUserTimezoneAndTimeSetWhenSaveThenTimeIsCustom() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30)

        transactionDraftService.setDate(profileId, localDate)
        transactionDraftService.setTime(profileId, localTime)

        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val transactionId = transactionDraftService.finishDraft(profileId, ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)

        assertEquals(localTime, transaction!!.time)
    }

    @Test
    fun givenSellerAndLocationSetIfLocationSomehowNotLinkedToSellerWhenSaveThenLocationNull() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val sellerId = sellerRepository.insertSeller(SellerPojo(null, profile.id!!, "Restaurant", utcDate))
        val sellerId2 = sellerRepository.insertSeller(SellerPojo(null, profile.id!!, "Coffee Shop", utcDate))
        val sellerLocationId = sellerLocationRepository.insertSellerLocation(SellerLocationPojo(null, sellerId, "Downtown", false.toByte(), null, null, null, utcDate))

        transactionDraftService.createDraft(profileId, accountId)


        transactionDraftService.setSeller(profileId, sellerId2)
        transactionDraftService.setSellerLocation(profileId, sellerLocationId)

        val transactionId = transactionDraftService.finishDraft(profileId, ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)!!

        assertNotNull(transaction.sellerId)
        assertNull(transaction.sellerLocationId)
    }

    companion object {
        val MOCKED_DATE = LocalDate.of(2025, 6, 30)
        val MOCKED_TIME = LocalTime.of(8, 0)
    }
}