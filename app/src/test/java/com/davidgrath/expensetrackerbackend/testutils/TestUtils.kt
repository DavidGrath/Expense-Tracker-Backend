package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.CORE_CATEGORIES
import com.davidgrath.expensetrackerbackend.configuration.TimeAndLocaleHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.toByte
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.junit.Assert
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val LOGGER = LoggerFactory.getLogger("com.davidgrath.expensetrackerbackend.testutils.TestUtils")

fun assertEqualsBD(expected: BigDecimal, actual: BigDecimal) {
    Assert.assertEquals("Expected $expected but was $actual", 0, expected.compareTo(actual))
}

fun file(vararg segments: String): File {
    val sep = File.separator
    val fullPath = segments.joinToString(sep)
    return File(fullPath)
}

fun file(file: File, vararg segments: String): File {
    val sep = File.separator
    val fullPath = segments.joinToString(sep)
    return File(file, fullPath)
}

fun getDefaultAccountId(profileRepository: ProfileRepository, accountRepository: AccountRepository): Long {
    val profile = profileRepository.findByStringId(Constants.DEFAULT_PROFILE_ID)!!
    val accountId = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!.id
    return accountId!!
}

fun tempInit(userRepository: UserRepository, profileRepository: ProfileRepository, accountRepository: AccountRepository, categoryRepository: CategoryRepository, timeAndLocaleHandler: TimeAndLocaleHandler): Unit {
    val user = UserPojo(null, Constants.DEFAULT_USER_ID, "user", "bcrypt:1234", "", LocalDateTime.now(timeAndLocaleHandler.getClock()))
    val id = userRepository.insertUser(user)
    val userDb = userRepository.findById(id)!!
    val profile = profileRepository.findByStringId(Constants.DEFAULT_PROFILE_ID)
        if(profile == null) {
            val defaultProfile = tempCreateDefaultProfile(userDb, profileRepository, timeAndLocaleHandler)
            tempInitProfile(defaultProfile, accountRepository, timeAndLocaleHandler)
            tempInitDefaultCategories(defaultProfile.id!!, categoryRepository, timeAndLocaleHandler)
        }
}

fun tempCreateDefaultProfile(user: UserPojo, profileRepository: ProfileRepository, timeAndLocaleHandler: TimeAndLocaleHandler): ProfilePojo {
    val clock = timeAndLocaleHandler.getClock()
    val date = ZonedDateTime.now(clock)
    val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
    val profile = ProfilePojo(null, user.id!!, "Default", Constants.DEFAULT_PROFILE_ID, utcDate)
    val id = profileRepository.insertProfile(profile)
    return profileRepository.findByStringId(Constants.DEFAULT_PROFILE_ID)!!
}

fun tempInitProfile(profile: ProfilePojo, accountRepository: AccountRepository, timeAndLocaleHandler: TimeAndLocaleHandler): Long {
    LOGGER.debug("tempInitProfile")

        val accounts = accountRepository.getAllByProfileId(profile.id!!)
        var defaultAccount = accounts.firstOrNull()
        val locale = timeAndLocaleHandler.getLocale()
        val currency = Currency.getInstance(locale)?.currencyCode ?: "USD"

        LOGGER.info("Picked default locale and currency: {}, {}", locale, currency)
        val clock = timeAndLocaleHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        if (defaultAccount == null) {
            //Create Default Account
            val account = AccountPojo(
                null,
                profile.id!!,
                currency,
                null,
                "",
                "Default Account",
                utcDate,
            )
            val id = accountRepository.insertAccount(account)
            defaultAccount = accountRepository.findById(id)!!
            LOGGER.info("Created default account for profile ${profile.stringId}")
        }
        return profile.id!!

}
fun tempInitDefaultCategories(profileId: Long, categoryRepository: CategoryRepository, timeAndLocaleHandler: TimeAndLocaleHandler): Unit {
    LOGGER.debug("tempInitDefaultCategories")

        val clock = timeAndLocaleHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        var anyCategoryNotExist = false
        for (category in CORE_CATEGORIES) {
            val categoryDb = categoryRepository.findByProfileIdAndStringId(profileId, category)
            if (categoryDb == null) {
                anyCategoryNotExist = true
                categoryRepository.insertCategory(
                    CategoryPojo(
                        null,
                        profileId,
                        category,
                        false.toByte(),
                        null,
                        utcDate
                    )
                )
            }
        }
        if(anyCategoryNotExist) {
            LOGGER.info("Created default categories")
        }

}

fun deleteAll(itemImagesRepository: ItemImagesRepository, itemRepository: TransactionItemRepository, imageRepository: ImageRepository, categoryRepository: CategoryRepository, transactionRepository: TransactionRepository, accountRepository: AccountRepository, profileRepository: ProfileRepository, userRepository: UserRepository) {
    itemImagesRepository.deleteAllForTesting()
    itemRepository.deleteAllForTesting()
    imageRepository.deleteAllForTesting()
    categoryRepository.deleteAllForTesting()
    transactionRepository.deleteAllForTesting()
    accountRepository.deleteAllForTesting()
    profileRepository.deleteAllForTesting()
    userRepository.deleteAllForTesting()
}