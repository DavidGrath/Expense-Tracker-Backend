package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.utils.*
import io.jsonwebtoken.Jwts
import org.junit.Assert
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

fun getDefaultAccountId(userRepository: UserRepository, profileRepository: ProfileRepository, accountRepository: AccountRepository): Long {

    val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
    val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
    val accountId = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!.id
    return accountId!!
}

fun createBearerToken(username: String, signingKey: String, algorithm: String, timeHandler: TimeHandler): String {
    val bytes = Base64.getDecoder().decode(signingKey)
    val keySpec = PKCS8EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance(algorithm)
    val key = keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    val localDateTime = LocalDateTime.now(timeHandler.getClock())
//        val expiry = localDateTime.plus(60, ChronoUnit.MINUTES)
    val jws = Jwts.builder()
        .subject(username)
        .signWith(key)
        .issuedAt(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
        .id(UUID.randomUUID().toString())
        .claim(Constants.CUSTOM_CLAIM_TYPE, Constants.TokenType.Access.value)
        .compact()
    return jws
}

fun createRefreshToken(username: String, signingKey: String, algorithm: String, timeHandler: TimeHandler): String {

    val bytes = Base64.getDecoder().decode(signingKey)
    val keySpec = PKCS8EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance(algorithm)
    val key = keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    val localDateTime = LocalDateTime.now(timeHandler.getClock())
//        val expiry = localDateTime.plus(60, ChronoUnit.DAYS)
    val jws = Jwts.builder()
        .subject(username)
        .signWith(key)
        .issuedAt(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
        .id(UUID.randomUUID().toString())
        .claim(Constants.CUSTOM_CLAIM_TYPE, Constants.TokenType.Refresh.value)
        .compact()
    return jws
}

fun tempInit(component: TestUtilComponent, timeHandler: TimeHandler): Unit {
    val userRepository = component.userRepository
    val profileRepository = component.profileRepository
    val accountRepository = component.accountRepository
    val categoryRepository = component.categoryRepository
    val user = UserPojo(null, TestConstants.DEFAULT_USER_ID, "username", "password", "", LocalDateTime.now(timeHandler.getClock()))
    val id = userRepository.insertUser(user)
    val userDb = userRepository.findById(id)!!
    val profile = profileRepository.findByStringId(userDb.id!!, Constants.DEFAULT_PROFILE_ID)
        if(profile == null) {
            val defaultProfile = tempCreateDefaultProfile(userDb, profileRepository, timeHandler)
            tempInitProfile(defaultProfile, accountRepository, timeHandler)
            tempInitDefaultCategories(defaultProfile.id!!, categoryRepository, timeHandler)
        }
}

fun tempCreateDefaultProfile(user: UserPojo, profileRepository: ProfileRepository, timeHandler: TimeHandler): ProfilePojo {
    val clock = timeHandler.getClock()
    val date = ZonedDateTime.now(clock)
    val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
    val profile = ProfilePojo(null, user.id!!, "Default", Constants.DEFAULT_PROFILE_ID, utcDate)
    val id = profileRepository.insertProfile(profile)
    return profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
}

fun tempInitProfile(profile: ProfilePojo, accountRepository: AccountRepository, timeHandler: TimeHandler): Long {
    LOGGER.debug("tempInitProfile")

        val accounts = accountRepository.getAllByProfileId(profile.id!!)
        var defaultAccount = accounts.firstOrNull()
        val locale = Locale.US
        val currency = Currency.getInstance(locale)?.currencyCode ?: "USD"

        LOGGER.info("Picked default locale and currency: {}, {}", locale, currency)
        val clock = timeHandler.getClock()
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
            defaultAccount = accountRepository.findByProfileIdAndId(profile.id!!, id)!!
            LOGGER.info("Created default account for profile ${profile.stringId}")
        }
        return profile.id!!

}
fun tempInitDefaultCategories(profileId: Long, categoryRepository: CategoryRepository, timeHandler: TimeHandler): Unit {
    LOGGER.debug("tempInitDefaultCategories")

        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

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
        if(anyCategoryNotExist) {
            LOGGER.info("Created default categories")
        }

}

fun copyControllerTempFile(tempFolder: File, classLoader: ClassLoader, vararg resources: TestData.Resource) {
    for (resource in resources) {
        val resourceInputStream = classLoader.getResourceAsStream(resource.resourceName)
        val file = File(tempFolder, resource.fileName)
        val outputStream = file.outputStream()
        val bytes = resourceInputStream.copyTo(outputStream)
        LOGGER.debug("Bytes copied: {}", bytes)
        resourceInputStream.close()
        outputStream.close()
    }
}

fun getHashCount(sha256: String, folder: File): Int {
    if(!folder.exists() || !folder.isDirectory) {
        LOGGER.debug("Exists: {}", folder.exists())
        LOGGER.debug("Is Dir: {}", folder.isDirectory())
        LOGGER.info("Invalid folder: {}", folder.path)
        return 0 //Maybe -1
    }

    var hashCount = 0
    for(f in folder.listFiles()!!) {
        LOGGER.info("F: ${f.absolutePath}")
        val inputStream = f.inputStream()
        val h = getSha256(inputStream)
        if(h == sha256) {
            hashCount++
        }
        inputStream.close()
    }
    return hashCount
}