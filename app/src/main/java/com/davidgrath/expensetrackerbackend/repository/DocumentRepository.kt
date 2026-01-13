package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.entities.views.DocumentWithTransactionDateAndOrdinal
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Document
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.DOCUMENT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import org.jooq.DSLContext
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class DocumentRepository {
    @Autowired
    open lateinit var dslContext: DSLContext

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

    fun findByTransactionId(profileId: Long, transactionId: Long): List<DocumentPojo> {
        val selectStep = dslContext.select(DOCUMENT.asterisk())
            .from(DOCUMENT)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(DOCUMENT.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(DOCUMENT.TRANSACTION_ID.eq(transactionId))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetch().into(DocumentPojo::class.java)
    }


    fun findByUserIdAndId(userId: Long, id: Long): DocumentPojo? {
        val selectStep = dslContext.select(DOCUMENT.asterisk())
            .from(DOCUMENT)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(DOCUMENT.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .innerJoin(PROFILE).on(PROFILE.ID.eq(ACCOUNT.PROFILE_ID))
            .where(DOCUMENT.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))

        return selectStep.fetchOne()?.into(DocumentPojo::class.java)
    }
    fun findByProfileIdAndId(profileId: Long, id: Long): DocumentWithTransactionDateAndOrdinal? {
        val selectStep = dslContext.select(
            DOCUMENT.ID, TRANSACTION.ID.`as`("transactionId"), DOCUMENT.TYPE,
            DOCUMENT.SIZE_BYTES, DOCUMENT.SHA_256, DOCUMENT.MIME_TYPE, DOCUMENT.PATH,
            TRANSACTION.DATED_AT.`as`("transactionDatedAt"), TRANSACTION.ORDINAL.`as`("transactionOrdinal")
        )
            .from(DOCUMENT)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(DOCUMENT.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(DOCUMENT.ID.eq(id))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(DocumentWithTransactionDateAndOrdinal::class.java)
    }

    fun findAll(profileId: Long): List<DocumentWithTransactionDateAndOrdinal> {
        val selectStep = dslContext.select(
            DOCUMENT.ID, TRANSACTION.ID.`as`("transactionId"), DOCUMENT.TYPE,
            DOCUMENT.SIZE_BYTES, DOCUMENT.SHA_256, DOCUMENT.MIME_TYPE, DOCUMENT.PATH,
            TRANSACTION.DATED_AT.`as`("transactionDatedAt"), TRANSACTION.ORDINAL.`as`("transactionOrdinal")
        )
            .from(DOCUMENT)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(DOCUMENT.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .where(ACCOUNT.PROFILE_ID.eq(profileId))
            .limit(1_000)
        return selectStep.fetch().into(DocumentWithTransactionDateAndOrdinal::class.java)
    }

    fun getStats(profileId: Long): ImageDocumentStats {
        val selectStep = dslContext.select(
            DSL.count(DOCUMENT.ID).`as`("count"),
            DSL.ifnull(DSL.sum(DOCUMENT.SIZE_BYTES), 0L).`as`("totalSizeBytes")
        )
            .from(DOCUMENT)
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(DOCUMENT.TRANSACTION_ID))
            .innerJoin(ACCOUNT).on(ACCOUNT.ID.eq(TRANSACTION.ACCOUNT_ID))
            .and(ACCOUNT.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(ImageDocumentStats::class.java)!!
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(DOCUMENT).where("true")
        return delete.execute()
    }

}