package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.DocumentRepository
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.TestRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserDraftRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestComponent

@TestComponent
class TestUtilComponent {

    @Autowired
    lateinit var testRepository: TestRepository

    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var itemRepository: TransactionItemRepository
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var documentRepository: DocumentRepository
    @Autowired
    lateinit var userDraftRepository: UserDraftRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository


    fun deleteAll() {
        testRepository.deleteAllUserDrafts()
        testRepository.deleteAllItemImages()
        testRepository.deleteAllDocuments()
        testRepository.deleteAllItems()
        testRepository.deleteAllImages()
        testRepository.deleteAllCategories()
        testRepository.deleteAllTransactions()
        testRepository.deleteAllAccounts()
        testRepository.deleteAllSellerLocations()
        testRepository.deleteAllSellers()
        testRepository.deleteAllProfiles()
        testRepository.deleteAllUsers()
    }
}