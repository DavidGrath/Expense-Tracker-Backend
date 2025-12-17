package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Document
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import org.jooq.DSLContext
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

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(DOCUMENT).where("true")
        return delete.execute()
    }
}