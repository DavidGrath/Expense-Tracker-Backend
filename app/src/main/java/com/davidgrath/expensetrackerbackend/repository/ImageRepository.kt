package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.entities.views.ImageWithStats
import com.davidgrath.expensetrackerbackend.entities.views.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM_IMAGES
import org.jooq.DSLContext
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
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


    fun findByUserIdAndId(userId: Long, id: Long): ImagePojo? {
        val selectStep = dslContext.select(IMAGE.asterisk())
            .from(IMAGE)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(IMAGE.PROFILE_ID))
            .where(IMAGE.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))

        return selectStep.fetchOne()?.into(ImagePojo::class.java)
    }
    fun findByProfileIdAndId(profileId: Long, id: Long): ImageWithStats? {
        val selectStep = dslContext.select(
            IMAGE.ID, IMAGE.PROFILE_ID, IMAGE.SIZE_BYTES,
            IMAGE.SHA_256, IMAGE.MIME_TYPE, IMAGE.PATH,
            dslContext.select(
                DSL.countDistinct(TRANSACTION.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
                .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.ID.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID))
                .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("transactionCount"),
            dslContext.select(
                DSL.count(TRANSACTION_ITEM.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
                .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.ID.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID))
                .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("itemCount")
        )
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(IMAGE.ID.eq(id))
        return selectStep.fetchOne()?.into(ImageWithStats::class.java)
    }

    fun findAll(profileId: Long): List<ImageWithStats> {
        val selectStep = dslContext.select(
            IMAGE.ID, IMAGE.PROFILE_ID, IMAGE.SIZE_BYTES,
            IMAGE.SHA_256, IMAGE.MIME_TYPE, IMAGE.PATH,
            dslContext.select(
                DSL.countDistinct(TRANSACTION.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
                .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.ID.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID))
                .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("transactionCount"),
            dslContext.select(
                DSL.count(TRANSACTION_ITEM.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
                .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.ID.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID))
                .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("itemCount")
        )
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .limit(1_000)
        return selectStep.fetch().into(ImageWithStats::class.java)
    }

    fun getOverallStats(profileId: Long): ImageDocumentStats {
        val selectStep = dslContext.select(
            DSL.count(IMAGE.ID).`as`("count"),
            DSL.ifnull(DSL.sum(IMAGE.SIZE_BYTES), 0L).`as`("totalSizeBytes")
        )
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(ImageDocumentStats::class.java)!!
    }

    fun getImageStats(profileId: Long, id: Long): TransactionAndItemCount {
        val selectStep = dslContext.select(
            DSL.countDistinct(TRANSACTION.ID).`as`("transactionCount"),
            DSL.count(TRANSACTION_ITEM.ID).`as`("itemCount")
        ).from(IMAGE)
            .innerJoin(TRANSACTION_ITEM_IMAGES).on(TRANSACTION_ITEM_IMAGES.IMAGE_ID.eq(IMAGE.ID))
            .innerJoin(TRANSACTION_ITEM).on(TRANSACTION_ITEM.ID.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ID))
            .innerJoin(TRANSACTION).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.TRANSACTION_ID))
            .where(IMAGE.ID.eq(id))
            .and(IMAGE.PROFILE_ID.eq(profileId))
            .orderBy(TRANSACTION_ITEM_IMAGES.CREATED_AT.desc())

        return selectStep.fetchOne()?.into(TransactionAndItemCount::class.java)!!
    }




    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(IMAGE).where("true")
        return delete.execute()
    }
}