package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import org.jooq.DSLContext
import org.jooq.conf.ParamType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class ImageRepository {

    @Autowired
    open lateinit var dslContext: DSLContext
    open var IMAGE: Image = Image.IMAGE

    fun insertImage(image: ImagePojo): Long {
        val imageDb = dslContext.newRecord(IMAGE, image)
        imageDb.store()
        return imageDb.id!!
    }

    fun findBySha256(profileId: Long, sha256: String): ImagePojo? {
        val selectStep = dslContext.select(IMAGE.asterisk())
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(IMAGE.SHA_256.eq(sha256))
        return selectStep.fetchOne()?.into(ImagePojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(IMAGE).where("true")
        return delete.execute()
    }
}