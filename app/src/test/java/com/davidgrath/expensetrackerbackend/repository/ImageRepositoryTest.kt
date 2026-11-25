package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestData
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.util.HexFormat
import java.util.UUID


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
class ImageRepositoryTest {

    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
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
    fun getImagesWithStatsTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val dumbbells = TestData.Resource.Images.DUMBBELLS_1
        val dumbbells2 = TestData.Resource.Images.DUMBBELLS_2
        val bread = TestData.Resource.Images.BREAD

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        dataBuilder.createTransaction()
            .withDetailedItem().mainDetails("Dumbbells", "fitness", BigDecimal(100))
            .withImages(dumbbells).build()
            .withDetailedItem().mainDetails("More Dumbbells", "fitness", BigDecimal(100))
            .withImages(dumbbells).build()
            .atDate(LocalDate.now(timeHandler.getClock()))
            .commit()

        dataBuilder.createTransaction()
            .debitOrCredit(false)
            .withDetailedItem().mainDetails("Even more dumbbells", "fitness", BigDecimal(100))
            .withImages(dumbbells, dumbbells2).build()
            .withDetailedItem().mainDetails("Bread", "food", BigDecimal(100))
            .withImages(bread).build()
            .atDate(LocalDate.now(timeHandler.getClock()))
            .commit()

        val imagesWithStats = imageRepository.findAll(profile!!.id!!)
        val dumbbellStats = imagesWithStats.find { it.sha256.contentEquals(HexFormat.of().parseHex(dumbbells.sha256)) }!!
        assertEquals(2, dumbbellStats.transactionCount)
        assertEquals(3, dumbbellStats.itemCount)

        val dumbbell2Stats = imagesWithStats.find { it.sha256.contentEquals(HexFormat.of().parseHex(dumbbells2.sha256)) }!!
        assertEquals(1, dumbbell2Stats.transactionCount)
        assertEquals(1, dumbbell2Stats.itemCount)
        val breadStats = imagesWithStats.find { it.sha256.contentEquals(HexFormat.of().parseHex(bread.sha256)) }!!
        assertEquals(1, breadStats.transactionCount)
        assertEquals(1, breadStats.itemCount)
    }
}