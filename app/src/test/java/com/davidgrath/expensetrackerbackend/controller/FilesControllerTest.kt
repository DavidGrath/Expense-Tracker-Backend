package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.getSha256
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestData
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertEquals
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
import org.springframework.web.util.DefaultUriBuilderFactory
import java.io.File
import java.nio.file.Files
import java.util.Locale

@RunWith(SpringRunner::class)
@WebMvcTest(AuthController::class)
@ContextConfiguration(
    classes = [
        FilesController::class, SecurityConfiguration::class, TestConfiguration::class, ExceptionResponseHandler::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FilesControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test
    fun simpleNotFoundTest() {
        val rootFolder = Files.createTempDirectory("temp").toFile()
        rootFolder.deleteOnExit()
        timeHandler.changeRoot(rootFolder)


        val userId = userService.createUser("username", "password", Locale.US)
        val user2Id = userService.createUser("user2", "password", Locale.US)
        val userUuid = userService.getByUsername("username")!!.uuid
        val userProfile = profileRepository.findAllByUserId(userId)[0]

        val documentsPath = "profiles/${userProfile.stringId}/data/documents"
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val filePath = "$documentsPath/${resource.fileName}"
        val documentsFolder = file(rootFolder, "users", userUuid!!, Constants.FOLDER_NAME_PROFILES, userProfile.stringId!!,
            Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS)
        documentsFolder.mkdirs()
        val document = File(documentsFolder, resource.fileName)
        val classLoader = FilesControllerTest::class.java.classLoader
        val resourceInputStream = classLoader.getResourceAsStream(resource.resourceName)

        val outputStream = document.outputStream()
        resourceInputStream.copyTo(outputStream)
        val path = document.path
        val uriPath = "/files/$filePath"
        val defaultUriBuilderFactory = DefaultUriBuilderFactory()
        defaultUriBuilderFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES
        val p = defaultUriBuilderFactory.uriString(uriPath).build()
        val jws = createBearerToken("user2", jwtSigningKey!!, JWT_ALG, timeHandler)
        val pathResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get(p)
                    .header("Authorization", "Bearer ${jws}")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNotFound(),
                MockMvcResultMatchers.jsonPath(
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code))
            )
            .andReturn();
    }

    @Test
    fun simple200Test() {


        val rootFolder = Files.createTempDirectory("temp").toFile()
        rootFolder.deleteOnExit()
        timeHandler.changeRoot(rootFolder)


        val userId = userService.createUser("username", "password", Locale.US)
        val userUuid = userService.getByUsername("username")!!.uuid
        val userProfile = profileRepository.findAllByUserId(userId)[0]

        val documentsPath = "profiles/${userProfile.stringId}/data/documents"
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val filePath = "$documentsPath/${resource.fileName}"
        val documentsFolder = file(rootFolder, "users", userUuid!!, Constants.FOLDER_NAME_PROFILES, userProfile.stringId!!,
            Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS)
        documentsFolder.mkdirs()
        val document = File(documentsFolder, resource.fileName)
        val classLoader = FilesControllerTest::class.java.classLoader
        val resourceInputStream = classLoader.getResourceAsStream(resource.resourceName)

        val outputStream = document.outputStream()
        resourceInputStream.copyTo(outputStream)
        val bytes = classLoader.getResourceAsStream(resource.resourceName).readBytes()
        val path = document.path
        val uriPath = "/files/$filePath"
        val defaultUriBuilderFactory = DefaultUriBuilderFactory()
        defaultUriBuilderFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES
        val p = defaultUriBuilderFactory.uriString(uriPath).build()
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val pathResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get(p)
                    .header("Authorization", "Bearer ${jws}")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk()
            )
            .andReturn();
        val content = pathResult.response.contentAsByteArray
        val hash = getSha256(content)
        assertEquals(resource.sha256, hash)
    }
}