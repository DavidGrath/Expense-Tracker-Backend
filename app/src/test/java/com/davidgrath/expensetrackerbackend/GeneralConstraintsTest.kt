package com.davidgrath.expensetrackerbackend

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.DocumentType
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.DocumentRepository
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.jooq.exception.DataAccessException
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.HexFormat
import java.util.UUID


/**
 * Meant to test nontrivial database-level `CHECK` constraints
 */
@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ]
)
class GeneralConstraintsTest {

    companion object {
        /**
         * TODO Maybe I should use the information_schema.check_constraints view to generate these names so I won't have to remember to update them manually whenever the schema changes
         *
         * select table_name,constraint_name,constraint_type from information_schema.table_constraints tc where tc.constraint_schema='expense_tracker_database_testlocal' and constraint_type="CHECK"
         */
        const val CONSTRAINT_NAME_USER_USERNAME_VALID = "chk_user_username_valid"
        const val CONSTRAINT_NAME_PROFILE_NAME_VALID = "chk_profile_name_valid"
        const val CONSTRAINT_NAME_CATEGORY_IS_CUSTOM_VALID = "chk_category_is_custom_valid"
        const val CONSTRAINT_NAME_CATEGORY_NAME_VALID = "chk_category_name_valid"
        const val CONSTRAINT_NAME_ACCOUNT_CURRENCY_CODE_VALID = "chk_account_currency_code_valid"
        const val CONSTRAINT_NAME_ACCOUNT_NAME_VALID = "chk_account_name_valid"
        const val CONSTRAINT_NAME_SELLER_NAME_VALID = "chk_seller_name_valid"
        const val CONSTRAINT_NAME_SELLER_LOCATION_NAME_VALID = "chk_seller_location_name_valid"
//        const val CONSTRAINT_NAME_SELLER_LOCATION_LONGITUDE_VALID = "chk_seller_location_longitude_valid"
//        const val CONSTRAINT_NAME_SELLER_LOCATION_LATITUDE_VALID = "chk_seller_location_latitude_valid"
        const val CONSTRAINT_NAME_SELLER_LOCATION_COORDINATES_VALID = "chk_seller_location_coordinates_valid"
        const val CONSTRAINT_NAME_SELLER_LOCATION_ADDRESS_VALID = "chk_seller_location_address_valid"
//        const val CONSTRAINT_NAME_TRANSACTION_AMOUNT_VALID = "chk_transaction_amount_valid"
        const val CONSTRAINT_NAME_TRANSACTION_SELLER_LOCATION_ORDINAL_VALID = "chk_transaction_seller_location_ordinal_valid"
        const val CONSTRAINT_NAME_TRANSACTION_NOTE_VALID = "chk_transaction_note_valid"
//        const val CONSTRAINT_NAME_TRANSACTION_ITEM_AMOUNT_VALID = "chk_transaction_item_amount_valid"
        const val CONSTRAINT_NAME_TRANSACTION_ITEM_DESCRIPTION_VALID = "chk_transaction_item_description_valid"
        const val CONSTRAINT_NAME_TRANSACTION_ITEM_BRAND_VALID = "chk_transaction_item_brand_valid"
        const val CONSTRAINT_NAME_TRANSACTION_ITEM_VARIATION_VALID = "chk_transaction_item_variation_valid"
//        const val CONSTRAINT_NAME_TRANSACTION_ITEM_QUANTITY_VALID = "chk_transaction_item_quantity_valid"
        const val CONSTRAINT_NAME_TRANSACTION_ITEM_IS_REDUCTION_VALID = "chk_transaction_item_is_reduction_valid"
        const val CONSTRAINT_NAME_TRANSACTION_ITEM_WEIGHT_VALID = "chk_transaction_item_weight_valid"
        const val CONSTRAINT_NAME_TRANSACTION_ITEM_VOLUME_VALID = "chk_transaction_item_volume_valid"
        //const val CONSTRAINT_NAME_TRANSACTION_ITEM_OCCUPANCY_SLOT_VALID = "chk_transaction_item_occupancy_slot_valid"
//        const val CONSTRAINT_NAME_IMAGE_SIZE_BYTES_VALID = "chk_image_size_bytes_valid"
        const val CONSTRAINT_NAME_IMAGE_MIME_TYPE_VALID = "chk_image_mime_type_valid"
//        const val CONSTRAINT_NAME_DOCUMENT_SIZE_BYTES_VALID = "chk_document_size_bytes_valid"
        const val CONSTRAINT_NAME_DOCUMENT_MIME_TYPE_VALID = "chk_document_mime_type_valid"
        //const val CONSTRAINT_NAME_DOCUMENT_OCCUPANCY_SLOT_VALID = "chk_document_occupancy_slot_valid"
        //const val CONSTRAINT_NAME_TRANSACTION_ITEM_IMAGE_SLOT_VALID = "chk_transaction_item_image_slot_valid"
    }

    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var itemRepository: TransactionItemRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var documentRepository: DocumentRepository

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    val alrightHash = "456968c5c0fdd5ed64acc508315ed439ce9f4b7ab0c7ae7c124582d2c3d9c1fc"
    val alrightHashBytes = HexFormat.of().parseHex(alrightHash)

    /**
     * Please refer to Unicode TR44 for the full list of general categories
     */
    val controlChar = "\u0001"
    val mark = "\u0300"
    val separators = " \u2028\u2029"
    val softHyphen = '\u00ad'
    val format = "$softHyphen"
    val completeNonsenseString = "$mark$separators$format"
    val controlCharsWhitespace = "\r\n\t"
    val nameWithMark = "Valid Name$mark"
    val nameWithControlCharsAndSeparators = "Control Name$controlChar$separators"
    val nameWithControlCharsWhitespace = "Space Name$controlCharsWhitespace"

    fun createProfile(): Pair<Long, LocalDateTime> {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDateTime = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), "username", "password", "", LocalDateTime.now(timeHandler.getClock()))
        val userId = userRepository.insertUser(user)
        val profile = ProfilePojo(null, userId, "Default", uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)), utcDateTime)
        val profileId = profileRepository.insertProfile(profile)
        return profileId to utcDateTime
    }

    @Test
    fun givenMimeTypeNotValidWhenSaveImageThenFailure() {
        val (profileId, dateTime) = createProfile()
        val noSlash = "imagejpeg"
        var invalidMimeType = noSlash
        var imageFile = ImagePojo(null, profileId, 1L, alrightHashBytes, invalidMimeType, "", "files/user", dateTime)
        var exception: DataAccessException? = try {
            imageRepository.insertImage(imageFile)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_IMAGE_MIME_TYPE_VALID)

        val mixedCase = "iMAGe/JPEg"
        invalidMimeType = mixedCase
        imageFile = ImagePojo(null, profileId, 1L, alrightHashBytes, invalidMimeType, "", "files/user", dateTime)
        exception = try {
            imageRepository.insertImage(imageFile)
            null
        } catch (e: DataAccessException) {
            e
        }
        CONSTRAINT_NAME_IMAGE_MIME_TYPE_VALID
    }

    @Test
    fun givenMimeTypeIsValidWhenSaveImageThenSuccess() {

        val (profileId, dateTime) = createProfile()
        val imageFile = ImagePojo(null, profileId, 1L, alrightHashBytes,"image/jpeg", "", "files/user", dateTime)
        val exception: DataAccessException? = try {
            imageRepository.insertImage(imageFile)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    @Test
    fun givenUsernameIsInvalidWhenSaveThenFailure() {

        var invalidUsername = "_name_123"
        var user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), invalidUsername, "password", "", LocalDateTime.now(timeHandler.getClock()))
        var exception: DataAccessException? = try {
            userRepository.insertUser(user)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_USER_USERNAME_VALID)
        )

        invalidUsername = "123_user_name"
        user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), invalidUsername, "password", "", LocalDateTime.now(timeHandler.getClock()))
        exception = try {
            userRepository.insertUser(user)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_USER_USERNAME_VALID)
        )

        invalidUsername = ""
        user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), invalidUsername, "password", "", LocalDateTime.now(timeHandler.getClock()))
        exception = try {
            userRepository.insertUser(user)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_USER_USERNAME_VALID)
        )

        invalidUsername = ""
        user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), invalidUsername, "password", "", LocalDateTime.now(timeHandler.getClock()))
        exception = try {
            userRepository.insertUser(user)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_USER_USERNAME_VALID)
        )

        invalidUsername = " \t"
        user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), invalidUsername, "password", "", LocalDateTime.now(timeHandler.getClock()))
        exception = try {
            userRepository.insertUser(user)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_USER_USERNAME_VALID)
        )
    }

    @Test
    fun givenUsernameIsValidWhenSaveThenSuccess() {
        val validUsername = "user_name_123"
        val user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), validUsername, "password", "", LocalDateTime.now(timeHandler.getClock()))
        val exception: DataAccessException? = try {
            userRepository.insertUser(user)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }


    @Test
    fun givenProfileNameIsInvalidWhenSaveThenFailure() {

        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDateTime = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), "username", "password", "", utcDateTime)
        val userId = userRepository.insertUser(user)

        val invalidNames = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for(invalidProfileName in invalidNames) {

            var profile = ProfilePojo(null, userId, invalidProfileName, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)), utcDateTime)
            var exception: DataAccessException? = try {
                profileRepository.insertProfile(profile)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_PROFILE_NAME_VALID)
        }
    }

    @Test
    fun givenProfileNameIsValidWhenSaveThenSuccess() {

        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDateTime = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user = UserPojo(null, uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)), "username", "password", "", utcDateTime)
        val userId = userRepository.insertUser(user)

        val validProfileName = nameWithMark
        val profile = ProfilePojo(null, userId, validProfileName, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)), utcDateTime)

        val exception: DataAccessException? = try {
            profileRepository.insertProfile(profile)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    @Test
    fun givenTransactionNoteIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)



        var invalidNote = "This is a $controlChar bad note"
        var transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other, invalidNote)

        var exception: DataAccessException? = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_NOTE_VALID)
        )

        invalidNote = completeNonsenseString
        transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other, invalidNote)

        exception = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_NOTE_VALID)
        )

        val empty = ""
        invalidNote = empty
        transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other, invalidNote)

        exception = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_NOTE_VALID)
        )
    }

    @Test
    fun givenTransactionNoteIsValidWhenSaveThenSuccess() {


        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)


        val validNote = "This is a good $controlCharsWhitespace note"
        val transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other, validNote)

        val exception: DataAccessException? = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }


    @Test
    fun givenAccountCurrencyCodeInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val tooLong = "USDD"
        var invalidCurrencyCode = tooLong
        var account = AccountPojo(null, profileId, invalidCurrencyCode, null, "", "Basic Account", dateTime)

        var exception: DataAccessException? = try {
            accountRepository.insertAccount(account)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        println(exception)
        val mySQLDataTooLong = 1406
        assertTrue(
            (exception!!.cause!! as SQLException).errorCode == mySQLDataTooLong
        )

        val lowerCase = "usd"
        invalidCurrencyCode = lowerCase

        account = AccountPojo(null, profileId, invalidCurrencyCode, null, "", "Basic Account", dateTime)

        exception = try {
            accountRepository.insertAccount(account)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_ACCOUNT_CURRENCY_CODE_VALID)
        )

        val notAlphabets = "123"
        invalidCurrencyCode = notAlphabets

        account = AccountPojo(null, profileId, invalidCurrencyCode, null, "", "Basic Account", dateTime)

        exception = try {
            accountRepository.insertAccount(account)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_ACCOUNT_CURRENCY_CODE_VALID)
        )

        val tooShort = "US"
        invalidCurrencyCode = tooShort

        account = AccountPojo(null, profileId, invalidCurrencyCode, null, "", "Basic Account", dateTime)

        exception = try {
            accountRepository.insertAccount(account)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_ACCOUNT_CURRENCY_CODE_VALID)
        )
    }

    @Test
    fun givenAccountCurrencyCodeIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()

        val validCurrencyCode = "USD"
        val account = AccountPojo(null, profileId, validCurrencyCode, null, "", "Basic Account", dateTime)

        val exception: DataAccessException? = try {
            accountRepository.insertAccount(account)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }

    @Test
    fun givenTransactionItemWeightIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        var weight: BigDecimal? = BigDecimal(100)
        var weightUnit: String? = null

        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, null, null,
            weight, weightUnit, null, null, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_ITEM_WEIGHT_VALID)
        )


        weight = null
        weightUnit = "g"

        item = item.copy(weight = weight, weightUnit = weightUnit)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_ITEM_WEIGHT_VALID)
        )
    }

    @Test
    fun givenTransactionItemWeightIsValidWhenSaveThenSuccess() {

        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)
        var weight: BigDecimal? = null
        var weightUnit: String? = null
        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, null, null,
            weight, weightUnit, null, null, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        weight = BigDecimal(100)
        weightUnit = "g"

        item = item.copy(weight = weight, weightUnit = weightUnit, sortKey = 2_000, occupancySlot = 2)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }

    @Test
    fun givenTransactionItemVolumeIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        var volume: BigDecimal? = BigDecimal(100)
        var volumeUnit: String? = null

        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, null, null,
            null, null, volume, volumeUnit, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_ITEM_VOLUME_VALID)
        )


        volume = null
        volumeUnit = "L"

        item = item.copy(volume = volume, volumeUnit = volumeUnit)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_TRANSACTION_ITEM_VOLUME_VALID)
        )
    }

    @Test
    fun givenTransactionItemVolumeIsValidWhenSaveThenSuccess() {

        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)
        var volume: BigDecimal? = null
        var volumeUnit: String? = null
        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, null, null,
            null, null, volume, volumeUnit, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        volume = BigDecimal(100)
        volumeUnit = "L" //TODO UCUM

        item = item.copy(volume = volume, volumeUnit = volumeUnit, sortKey = 2_000, occupancySlot = 2)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }

    @Test
    fun givenCategoryIsCustomIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()
        var isCustom: Byte = FALSE_BYTE
        var name: String? = "Name"
        var stringId: String? = null
        var category = CategoryPojo(null, profileId, stringId, isCustom, name, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        var exception: DataAccessException? = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_CATEGORY_IS_CUSTOM_VALID)
        )

        isCustom = TRUE_BYTE
        name = null
        stringId = "miscellaneous"
        category = CategoryPojo(null, profileId, stringId, isCustom, name, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        exception = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(CONSTRAINT_NAME_CATEGORY_IS_CUSTOM_VALID)
        )
    }

    @Test
    fun givenCategoryIsCustomIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()
        var isCustom: Byte = FALSE_BYTE
        var name: String? = null
        var stringId: String? = "miscellaneous"
        var category = CategoryPojo(null, profileId, stringId, isCustom, name, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        var exception: DataAccessException? = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        isCustom = TRUE_BYTE
        name = "Name"
        stringId = null
        category = CategoryPojo(null, profileId, stringId, isCustom, name, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        exception = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }


    @Test
    fun givenCategoryNameIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()
        val isCustom: Byte = TRUE_BYTE
        var invalidName = "${controlChar}Name"
        val stringId: String? = null
        var category = CategoryPojo(null, profileId, stringId, isCustom, invalidName, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        var exception: DataAccessException? = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_CATEGORY_NAME_VALID)

        invalidName = controlCharsWhitespace
        category = CategoryPojo(null, profileId, stringId, isCustom, invalidName, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        exception = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_CATEGORY_NAME_VALID)

        val empty = ""
        invalidName = empty
        category = CategoryPojo(null, profileId, stringId, isCustom, invalidName, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        exception = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_CATEGORY_NAME_VALID)


        invalidName = completeNonsenseString
        category = CategoryPojo(null, profileId, stringId, isCustom, invalidName, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        exception = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_CATEGORY_NAME_VALID)
    }


    @Test
    fun givenCategoryNameIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()
        val isCustom: Byte = TRUE_BYTE
        val validName = nameWithMark
        val stringId: String? = null
        val category = CategoryPojo(null, profileId, stringId, isCustom, validName, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")

        val exception: DataAccessException? = try {
            categoryRepository.insertCategory(category)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

    }


    @Test
    fun givenSellerCoordinatesInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        var longitude: Double? = null
        var latitude: Double? = 80.0

        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        var sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", longitude, latitude, null, dateTime)

        var exception: DataAccessException? = try {
            sellerLocationRepository.insertSellerLocation(sellerLocation)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_SELLER_LOCATION_COORDINATES_VALID)

        longitude = 170.0
        latitude = null


        sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", longitude, latitude, null, dateTime)

        exception = try {
            sellerLocationRepository.insertSellerLocation(sellerLocation)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_SELLER_LOCATION_COORDINATES_VALID)
    }


    @Test
    fun givenSellerCoordinatesValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()

        var longitude: Double? = 170.0
        var latitude: Double? = 80.0

        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        var sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", longitude, latitude, null, dateTime)

        var exception: DataAccessException? = try {
            sellerLocationRepository.insertSellerLocation(sellerLocation)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        longitude = null
        latitude = null


        sellerLocation = SellerLocationPojo(null, sellerId, 2, "Name", longitude, latitude, null, dateTime)

        exception = try {
            sellerLocationRepository.insertSellerLocation(sellerLocation)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }


    @Test
    fun givenMimeTypeNotValidWhenSaveDocumentThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val noSlash = "imagejpeg"
        var invalidMimeType = noSlash
        var document = DocumentPojo(null, transactionId, DocumentType.Other, 1L, alrightHashBytes, invalidMimeType, "", "", 1_000, 1, dateTime)

        var exception: DataAccessException? = try {
            documentRepository.insertDocument(document)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_DOCUMENT_MIME_TYPE_VALID)

        val mixedCase = "iMAGe/JPEg"
        invalidMimeType = mixedCase
        document = DocumentPojo(null, transactionId, DocumentType.Other, 1L, alrightHashBytes, invalidMimeType, "", "", 1_000, 1, dateTime)
        exception = try {
            documentRepository.insertDocument(document)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_DOCUMENT_MIME_TYPE_VALID)
    }

    @Test
    fun givenMimeTypeIsValidWhenSaveDocumentThenSuccess() {

        val (profileId, dateTime) = createProfile()
        val validMimeType = "image/jpeg"

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val document = DocumentPojo(null, transactionId, DocumentType.Other, 1L, alrightHashBytes, validMimeType, "", "", 1_000, 1, dateTime)

        val exception: DataAccessException? = try {
            documentRepository.insertDocument(document)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

    }

    @Test
    fun givenTransactionSellerLocationOrdinalInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()



        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        val sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", null, null, null, dateTime)
        val locationId = sellerLocationRepository.insertSellerLocation(sellerLocation)

        var transactionSeller: Long? = null
        var transactionSellerLocationOrdinal: Int? = 1
        var transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other, null,
            transactionSeller, transactionSellerLocationOrdinal, dateTime)

        var exception: DataAccessException? = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }
        println(exception)
        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_SELLER_LOCATION_ORDINAL_VALID)

    }

    @Test
    fun givenTransactionSellerLocationOrdinalValidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()



        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        val sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", null, null, null, dateTime)
        val locationId = sellerLocationRepository.insertSellerLocation(sellerLocation)

        var transactionSeller: Long? = sellerId
        var transactionSellerLocationOrdinal: Int? = 1
        var transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other, null,
            transactionSeller, transactionSellerLocationOrdinal, dateTime)

        var exception: DataAccessException? = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)

        transactionSeller = sellerId
        transactionSellerLocationOrdinal = null
        transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 2_000, TransactionMode.Other, null,
            transactionSeller, transactionSellerLocationOrdinal, dateTime)

        exception = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)


        transactionSeller = null
        transactionSellerLocationOrdinal = null
        transaction = TransactionPojo(null, accountId, BigDecimal(100), TRUE_BYTE, dateTime.toLocalDate(), null, 3_000, TransactionMode.Other, null,
            transactionSeller, transactionSellerLocationOrdinal, dateTime)

        exception = try {
            transactionRepository.insertTransaction(transaction)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    @Test
    fun givenTransactionItemIsReductionIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val isReduction = TRUE_BYTE
        var quantity: Int = 2
        var brand: String? = null
        var variation: String? = null
        var weight: BigDecimal? = null
        var weightUnit: String? = null
        var volume: BigDecimal? = null
        var volumeUnit: String? = null

        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_IS_REDUCTION_VALID)

        quantity = 1
        brand = "Brand"

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_IS_REDUCTION_VALID)

        brand = null
        variation = "Variation"

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_IS_REDUCTION_VALID)


        variation = null
        weight = BigDecimal(100)
        weightUnit = "g"

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_IS_REDUCTION_VALID)

        weight = null
        weightUnit = null
        volume = BigDecimal(100)
        volumeUnit = "L"

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_IS_REDUCTION_VALID)
    }

    @Test
    fun givenTransactionItemIsReductionIsValidWhenSaveThenSuccess() {

        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        var quantity: Int = 2
        var brand: String? = null
        var variation: String? = null
        var weight: BigDecimal? = null
        var weightUnit: String? = null
        var volume: BigDecimal? = null
        var volumeUnit: String? = null
        var sortKey = 1_000
        var occupancySlot = 1

        val isReduction = FALSE_BYTE

        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, sortKey, occupancySlot, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        quantity = 1
        brand = "Brand"
        sortKey += 1_000
        occupancySlot++

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, sortKey, occupancySlot, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        brand = null
        variation = "Variation"
        sortKey += 1_000
        occupancySlot++

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, sortKey, occupancySlot, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        variation = null
        weight = BigDecimal(100)
        weightUnit = "g"
        sortKey += 1_000
        occupancySlot++

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, sortKey, occupancySlot, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

        weight = null
        weightUnit = null
        volume = BigDecimal(100)
        volumeUnit = "L"
        sortKey += 1_000
        occupancySlot++

        item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, sortKey, occupancySlot, quantity, brand, variation,
            weight, weightUnit, volume, volumeUnit, isReduction, dateTime)

        exception = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)

    }

    @Test
    fun givenAccountNameIsInvalidWhenSaveThenFailure() {

        val (profileId, dateTime) = createProfile()

        val invalidNames = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for(invalidAccountName in invalidNames) {
            var account = AccountPojo(null, profileId, "USD", null, "", invalidAccountName, dateTime)

            var exception: DataAccessException? = try {
                accountRepository.insertAccount(account)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_ACCOUNT_NAME_VALID)
        }

    }

    @Test
    fun givenAccountNameIsValidWhenSaveThenSuccess() {

        val (profileId, dateTime) = createProfile()

        val validAccountName = nameWithMark
        val account = AccountPojo(null, profileId, "USD", null, "", validAccountName, dateTime)

        val exception: DataAccessException? = try {
            accountRepository.insertAccount(account)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    @Test
    fun givenSellerNameIsInValidWhenSaveFailure() {

        val (profileId, dateTime) = createProfile()

        val invalidNames = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for(invalidSellerName in invalidNames) {
            val seller = SellerPojo(null, profileId, invalidSellerName, dateTime)

            var exception: DataAccessException? = try {
                sellerRepository.insertSeller(seller)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_SELLER_NAME_VALID)
        }
    }

    @Test
    fun givenSellerNameIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()

        val validName = nameWithMark
        val seller = SellerPojo(null, profileId, validName, dateTime)

        var exception: DataAccessException? = try {
            sellerRepository.insertSeller(seller)
            null
        } catch (e: DataAccessException) {
            e
        }

        assertNull(exception)
    }

    @Test
    fun givenSellerLocationNameInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()


        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        val invalidNames = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for(invalidSellerLocationName in invalidNames) {
            var sellerLocation = SellerLocationPojo(null, sellerId, 1, invalidSellerLocationName, null, null, null, dateTime)

            var exception: DataAccessException? = try {
                sellerLocationRepository.insertSellerLocation(sellerLocation)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_SELLER_LOCATION_NAME_VALID)
        }
    }

    @Test
    fun givenSellerLocationNameValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()


        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        val validName = nameWithMark
        var sellerLocation = SellerLocationPojo(null, sellerId, 1, validName, null, null, null, dateTime)

        var exception: DataAccessException? = try {
            sellerLocationRepository.insertSellerLocation(sellerLocation)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    @Test
    fun givenSellerLocationAddressInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()


        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        val invalidAddresses = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for(invalidSellerLocationAddress in invalidAddresses) {
            var sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", null, null, invalidSellerLocationAddress, dateTime)

            var exception: DataAccessException? = try {
                sellerLocationRepository.insertSellerLocation(sellerLocation)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_SELLER_LOCATION_ADDRESS_VALID)
        }
    }

    @Test
    fun givenSellerLocationAddressValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()


        val seller = SellerPojo(null, profileId, "Name", dateTime)
        val sellerId = sellerRepository.insertSeller(seller)

        val validName = nameWithMark
        var sellerLocation = SellerLocationPojo(null, sellerId, 1, "Name", null, null, validName, dateTime)

        var exception: DataAccessException? = try {
            sellerLocationRepository.insertSellerLocation(sellerLocation)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    @Test
    fun givenTransactionItemDescriptionIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val invalidDescriptions = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for (invalidDescription in invalidDescriptions) {

            var item = TransactionItemPojo(null, transactionId, invalidDescription, transactionAmount, categoryId, 1_000, 1, 1, null, null,
                null, null, null, null, FALSE_BYTE, dateTime)

            var exception: DataAccessException? = try {
                itemRepository.insertTransactionItem(item)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_DESCRIPTION_VALID)
        }
    }

    @Test
    fun givenTransactionItemDescriptionIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val validDescription = nameWithMark
        var item = TransactionItemPojo(null, transactionId, validDescription, transactionAmount, categoryId, 1_000, 1, 1, null, null,
            null, null, null, null, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }


    @Test
    fun givenTransactionItemBrandIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val invalidBrands = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for (invalidBrand in invalidBrands) {

            var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, invalidBrand, null,
                null, null, null, null, FALSE_BYTE, dateTime)

            var exception: DataAccessException? = try {
                itemRepository.insertTransactionItem(item)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_BRAND_VALID)
        }
    }

    @Test
    fun givenTransactionItemBrandIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val validBrand = nameWithMark
        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, validBrand, null,
            null, null, null, null, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }


    @Test
    fun givenTransactionItemVariationIsInvalidWhenSaveThenFailure() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val invalidVariations = arrayOf(
            nameWithControlCharsAndSeparators,
            "",
            nameWithControlCharsWhitespace,
            completeNonsenseString
        )

        for (invalidVariation in invalidVariations) {

            var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, null, invalidVariation,
                null, null, null, null, FALSE_BYTE, dateTime)

            var exception: DataAccessException? = try {
                itemRepository.insertTransactionItem(item)
                null
            } catch (e: DataAccessException) {
                e
            }

            assertSQLCheckConstraint(exception, CONSTRAINT_NAME_TRANSACTION_ITEM_VARIATION_VALID)
        }
    }

    @Test
    fun givenTransactionItemVariationIsValidWhenSaveThenSuccess() {
        val (profileId, dateTime) = createProfile()

        val account = AccountPojo(null, profileId, "USD", null, "", "Basic Account", dateTime)
        val accountId = accountRepository.insertAccount(account)

        val transactionAmount = BigDecimal(100)
        val transaction = TransactionPojo(null, accountId, transactionAmount, TRUE_BYTE, dateTime.toLocalDate(), null, 1_000, TransactionMode.Other)
        val transactionId = transactionRepository.insertTransaction(transaction)

        val category = CategoryPojo(null, profileId, "miscellaneous", FALSE_BYTE, null, dateTime, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        val categoryId = categoryRepository.insertCategory(category)

        val validVariation = nameWithMark
        var item = TransactionItemPojo(null, transactionId, "Description", transactionAmount, categoryId, 1_000, 1, 1, null, validVariation,
            null, null, null, null, FALSE_BYTE, dateTime)

        var exception: DataAccessException? = try {
            itemRepository.insertTransactionItem(item)
            null
        } catch (e: DataAccessException) {
            e
        }
        assertNull(exception)
    }

    fun assertSQLCheckConstraint(exception: DataAccessException?, name: String) {
        assertNotNull(exception)
        assertTrue(exception!!.cause!! is SQLException)
        assertTrue(
            (exception!!.cause!! as SQLException).message!!
                .contains(name)
        )
    }
}