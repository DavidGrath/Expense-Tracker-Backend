package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Document
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import org.jooq.DSLContext
import org.jooq.conf.ParamType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class DocumentRepository {
    @Autowired
    open lateinit var dslContext: DSLContext
    open var DOCUMENT: Document = Document.DOCUMENT

    fun insertDocument(document: DocumentPojo): Long {
        val documentDb = dslContext.newRecord(DOCUMENT, document)
        documentDb.store()
        return documentDb.id!!
    }

    fun findByTransactionIdAndHash(profileId: Long, transactionId: Long, sha256: String): DocumentPojo? {
        val selectStep = dslContext.select(DOCUMENT.asterisk())
            .from(DOCUMENT)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(DOCUMENT.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(DOCUMENT.TRANSACTION_ID.eq(transactionId))
            .and(DOCUMENT.SHA_256.eq(sha256))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(DocumentPojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(DOCUMENT).where("true")
        return delete.execute()
    }

}