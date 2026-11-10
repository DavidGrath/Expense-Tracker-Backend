package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.entities.DocumentType
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.DocumentsApi
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.service.TransactionDraftServiceTest.Companion.MOCKED_DATE
import com.davidgrath.expensetrackerbackend.service.TransactionDraftServiceTest.Companion.MOCKED_TIME
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.util.*
import kotlin.toString

@RunWith(SpringRunner::class)
@WebMvcTest(DocumentsController::class)
@ContextConfiguration(
    classes = [
        DocumentsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DocumentsControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var documentRepository: DocumentRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Autowired
    lateinit var timeHandler: TimeHandler

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test
    fun getAllDocumentsOkTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = DocumentsControllerTest::class.java.classLoader

        copyControllerTempFile(tempFolder, classLoader, resource)

        createTransactionWithDocument(profileId, user.uuid!!, profile.uuid!!, accountId, tempFolder, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(DocumentsApi.PATH_DOCUMENTS_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.*", Matchers.iterableWithSize<Int>(Matchers.equalTo(1))
                )
            ).andReturn()
    }

    @Test
    fun getDocumentStatsOkTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = DocumentsControllerTest::class.java.classLoader

        copyControllerTempFile(tempFolder, classLoader, resource)

        createTransactionWithDocument(profileId, user.uuid!!, profile.uuid!!, accountId, tempFolder, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(DocumentsApi.PATH_DOCUMENTS_STATS_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.count", Matchers.equalTo<Int>(1)
                )
            ).andReturn()
    }

    @Test
    fun getDocumentByIdOkTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = DocumentsControllerTest::class.java.classLoader

        copyControllerTempFile(tempFolder, classLoader, resource)
        val userUuidString = user.uuid!!.toString()
        val profileUuidString = profile.uuid!!.toString()
        val documentId = createTransactionWithDocument(profileId, user.uuid!!, profile.uuid!!, accountId, tempFolder, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(DocumentsApi.PATH_DOCUMENTS_ID_GET, documentId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }

    fun createTransactionWithDocument(profileId: Long, userUuid: ByteArray, profileUuid: ByteArray, accountId: Long, tempFolder: File, resource: TestData.Resource): Long {
        val userUuidString = bytesToUUID(userUuid).toString()
        val profileUuidString = bytesToUUID(profileUuid).toString()
        val amount = BigDecimal.TEN
        val account = accountRepository.findByProfileIdAndId(profileId, accountId)!!


        val ordinalSortKeyOccupancy = transactionRepository.getMaxOrdinalSortKeyAndOccupancySlot(profileId, accountId)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
        val transaction = TransactionPojo(null, profileId, amount, true.toByte(), MOCKED_DATE, MOCKED_TIME,
            TransactionMode.Other, null, account.ordinal!!, null, null, nextOrdinal, nextSortKey, nextOccupancy, MOCKED_DATE.atTime(MOCKED_TIME),)

        val transactionId = transactionRepository.insertTransaction(transaction)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        val item = TransactionItemPojo(null, profileId, account.ordinal, nextOrdinal, "Basic", amount, misc.ordinal!!, 1, 1_000, 1, 1, null,
            null, null, null, null, null,false.toByte(),
            MOCKED_DATE.atTime(MOCKED_TIME))
        val itemId = transactionItemRepository.insertTransactionItem(item)
        //TODO make this more convenient
        val newFileName = "${UUID.randomUUID()}.jpg"
        val newPath = "${Constants.FOLDER_NAME_PROFILES}/$profileUuidString/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_DOCUMENTS}/${newFileName}"
        val destination = file(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS, userUuidString, Constants.FOLDER_NAME_PROFILES, profileUuidString, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, newFileName)

        val tempFile = File(tempFolder, resource.fileName)
        val length = tempFile.length()
        tempFile.copyTo(destination)
        tempFile.delete()
        val document = DocumentPojo(null, transactionId, DocumentType.Other, length, HexFormat.of().parseHex(resource.sha256), "image/jpeg", "", newPath, 1_000, 1, MOCKED_DATE.atTime(MOCKED_TIME))
        val documentId = documentRepository.insertDocument(document)
        return documentId
    }
}