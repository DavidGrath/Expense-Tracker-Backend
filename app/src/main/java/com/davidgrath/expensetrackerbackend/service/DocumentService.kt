package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.entities.views.DocumentWithTransactionDateAndOrdinal
import com.davidgrath.expensetrackerbackend.entities.views.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.repository.DocumentRepository
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DocumentService {

    @Autowired
    lateinit var documentRepository: DocumentRepository

    fun getDocumentsByTransactionId(profileId: Long, transactionId: Long): List<DocumentPojo> {
        val documents = documentRepository.findByTransactionId(profileId, transactionId)
        LOGGER.info("Documents size: {}", documents.size)
        return documents
    }

    //TODO Generic ownership check framework
    fun isDocumentForProfile(profileId: Long, documentId: Long): Boolean {
        val document = documentRepository.findByProfileIdAndId(profileId, documentId)
        val value = document != null
        LOGGER.info("Document for profile: {}", value)
        return value
    }

    fun isDocumentForUser(userId: Long, documentId: Long): Boolean {
        val document = documentRepository.findByUserIdAndId(userId, documentId)
        val value = document != null
        LOGGER.info("Document for user: {}", value)
        return value
    }

    /**
     * Limit 1_000
     */
    fun getAllUserDocuments(profileId: Long): List<DocumentWithTransactionDateAndOrdinal> {
        val documents = documentRepository.findAll(profileId)
        LOGGER.info("Documents size: {}", documents.size)
        return documents
    }

    fun getByProfileIdAndId(profileId: Long, documentId: Long): DocumentWithTransactionDateAndOrdinal? {
        return documentRepository.findByProfileIdAndId(profileId, documentId)
    }

    fun getDocumentStats(profileId: Long): ImageDocumentStats {
        return documentRepository.getStats(profileId)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DocumentService::class.java)
    }
}