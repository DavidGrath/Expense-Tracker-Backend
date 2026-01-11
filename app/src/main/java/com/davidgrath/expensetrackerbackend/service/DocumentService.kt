package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.repository.DocumentRepository
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DocumentService {

    lateinit var documentRepository: DocumentRepository

    fun getDocumentsByTransactionId(profileId: Long, transactionId: Long): List<DocumentPojo> {
        val documents = documentRepository.findByTransactionId(profileId, transactionId)
        LOGGER.info("Documents size: {}", documents.size)
        return documents
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DocumentService::class.java)
    }
}