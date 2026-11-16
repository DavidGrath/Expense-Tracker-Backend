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
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE

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


        //Create User
        val encryptedPassword = passwordEncoder.encode(password)
        val uuidX = UUID.randomUUID()
        val uuid = uuidToBytes(uuidX) //TODO JUG UUIDv7
        var userIdRet = -1L
        profileRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val user = UserPojo(null, uuid, username, encryptedPassword, "", utcDate)
            val userId = userRepository.insertUserTrx(trxContext, user)


            LOGGER.info("Created user {}", uuidX)

            //Create profile
            val profileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))

            val nextProfileSortKey: Short = 1_000
            val nextProfileOccupancySlot: Short = 1
            val profile = ProfilePojo(null, userId, "Default Profile", profileUuid, nextProfileSortKey, nextProfileOccupancySlot, utcDate)
            val profileId = profileRepository.insertProfileTrx(trxContext, profile)
            LOGGER.info("Created default profile for user {}", uuidX)


            //Create Accounts
            val currency = Currency.getInstance(locale)?.currencyCode ?: "USD"
            val nextAccountOrdinal: Short = 1
            val nextAccountSortKey: Short = 1_000
            val nextAccountOccupancySlot: Short = 1
            val account = AccountPojo(null, profileId, currency, null, null, "Default Account", TRUE_BYTE, nextAccountOrdinal, nextAccountSortKey, nextAccountOccupancySlot,utcDate)

            accountRepository.insertAccountTrx(trxContext, account) //TODO I made a mistake here where I didn't use the transaction context and it caused a lock wait timeout in 11 of my tests. I need to find a better way to make it more consistent
            LOGGER.info("Created default account with currency code {}", currency)
            //Create categories

            CORE_CATEGORIES.forEachIndexed { i, category ->
                val nextOrdinal = (i + 1).toShort()
                val nextSortKey = ((i + 1) * 1_000).toShort()
                val nextOccupancySlot = (i + 1).toShort()
                    val defaultIcon = CATEGORY_IDS_ICONS_DEFAULT[category]!!

                    categoryRepository.insertCategoryTrx(trxContext,
                        CategoryPojo(
                            null,
                            profileId,
                            category,
                            false.toByte(),
                            null,
                            defaultIcon,
                            nextOrdinal,
                            nextSortKey,
                            nextOccupancySlot,
                            utcDate
                        )
                    )

            }
            LOGGER.info("Created default categories")
            userIdRet = userId
        }

        return userIdRet
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