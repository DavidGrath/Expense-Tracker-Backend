package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.utils.CATEGORY_IDS_ICONS_DEFAULT
import com.davidgrath.expensetrackerbackend.utils.CORE_CATEGORIES
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.toByte
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class UserService {

    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    fun createUser(username: String, password: String, locale: Locale): Long {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        //TODO Transaction
        //Create User
        val encryptedPassword = passwordEncoder.encode(password)
        val uuid = uuidToBytes(UUID.randomUUID()) //TODO JUG UUIDv7
        val user = UserPojo(null, uuid, username, encryptedPassword, "", utcDate)
        val userId = userRepository.insertUser(user)

        LOGGER.info("Created user {}", username)

        //Create profile
        val profileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val profile = ProfilePojo(null, userId, "Default Profile", profileUuid, utcDate)
        val profileId = profileRepository.insertProfile(profile)
        LOGGER.info("Created default profile for user {}", username)

        //Create Accounts
        val currency = Currency.getInstance(locale)?.currencyCode ?: "USD"
        val account = AccountPojo(null, profileId, currency, null, null, "Default Account", utcDate)
        //TODO start transaction
        accountRepository.insertAccount(account)
        LOGGER.info("Created default account with currency code {}", currency)
        //Create categories

        var anyCategoryNotExist = false
        for (category in CORE_CATEGORIES) {
            val categoryDb = categoryRepository.findByProfileIdAndStringId(profileId, category)
            if (categoryDb == null) {
                val defaultIcon = CATEGORY_IDS_ICONS_DEFAULT[category]!!
                anyCategoryNotExist = true
                categoryRepository.insertCategory(
                    CategoryPojo(
                        null,
                        profileId,
                        category,
                        false.toByte(),
                        null,
                        utcDate,
                        defaultIcon
                    )
                )
            }
        }
        LOGGER.info("Created default categories")
        LOGGER.info("Any category not exist: {}", anyCategoryNotExist)
        return userId
    }

    fun doesUserExist(username: String): Boolean {
        val user = userRepository.findByUsername(username)
        return user != null
    }

    fun getByUsername(username: String): UserPojo? {
        return userRepository.findByUsername(username)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(UserService::class.java)
    }
}