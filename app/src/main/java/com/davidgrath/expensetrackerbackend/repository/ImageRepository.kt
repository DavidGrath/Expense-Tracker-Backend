package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.dtos.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.entities.dtos.ImageWithStats
import com.davidgrath.expensetrackerbackend.entities.dtos.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Image
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM_IMAGES
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class ImageRepository {

    @Autowired
    open lateinit var mainContext: DSLContext
    open var IMAGE: Image = Image.IMAGE

    fun insertImage(image: ImagePojo): Long {
        return insertImageTrx(mainContext, image)
    }

    fun insertImageTrx(trxContext: DSLContext, image: ImagePojo): Long {
        val imageDb = trxContext.newRecord(IMAGE, image)
        imageDb.store()
        return imageDb.id!!
    }

    fun findBySha256(profileId: Long, sha256: ByteArray): ImagePojo? {
        return findBySha256Trx(mainContext, profileId, sha256)
    }

    fun findBySha256Trx(trxContext: DSLContext, profileId: Long, sha256: ByteArray): ImagePojo? {
        val selectStep = trxContext.select(IMAGE.fields().toList()) //TO work around the generated columns
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(IMAGE.SHA_256.eq(sha256))
        return selectStep.fetchOne()?.into(ImagePojo::class.java)
    }


    fun findByUserIdAndId(userId: Long, id: Long): ImagePojo? {
        val selectStep = mainContext.select(IMAGE.fields().toList()) //TO work around the generated columns
            .from(IMAGE)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(IMAGE.PROFILE_ID))
            .where(IMAGE.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))

        return selectStep.fetchOne()?.into(ImagePojo::class.java)
    }
    fun findByProfileIdAndId(profileId: Long, id: Long): ImageWithStats? {
        val selectStep = mainContext.select(
            IMAGE.ID, IMAGE.PROFILE_ID, IMAGE.SIZE_BYTES,
            IMAGE.SHA_256, IMAGE.MIME_TYPE, IMAGE.PATH,
            mainContext.select(  //TODO Candidate for lateral join rework. I need a test, though
                DSL.countDistinct(TRANSACTION.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
                .innerJoin(TRANSACTION_ITEM).on(itemToItemImagesPath)
                .innerJoin(TRANSACTION).on(itemToTransactionPath) //TODO Candidate for reworking into a single join clause
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("transactionCount"),
            mainContext.select(
                DSL.count(TRANSACTION_ITEM.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
                .innerJoin(TRANSACTION_ITEM)
                .on(
                    TRANSACTION_ITEM.ORDINAL.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ORDINAL)
                        .and(TRANSACTION_ITEM.PROFILE_ID.eq(TRANSACTION_ITEM_IMAGES.PROFILE_ID))
                )
                .innerJoin(TRANSACTION)
                .on(TRANSACTION.ORDINAL.eq(TRANSACTION_ITEM.TRANSACTION_ORDINAL)
                    .and(TRANSACTION.PROFILE_ID.eq(TRANSACTION_ITEM_IMAGES.PROFILE_ID))) //TODO Candidate for reworking into a single join clause
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("itemCount")
        )
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(IMAGE.ID.eq(id))
        return selectStep.fetchOne()?.into(ImageWithStats::class.java)
    }

    fun findAll(profileId: Long): List<ImageWithStats> {
        val selectStep = mainContext.select(
            IMAGE.ID, IMAGE.PROFILE_ID, IMAGE.SIZE_BYTES,
            IMAGE.SHA_256, IMAGE.MIME_TYPE, IMAGE.PATH, //TODO candidate for lateral
            mainContext.select(
                DSL.countDistinct(TRANSACTION.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
                .innerJoin(TRANSACTION_ITEM).on(itemToItemImagesPath)
                .innerJoin(TRANSACTION).on(itemToTransactionPath) //TODO Candidate for reworking into a single join clause
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("transactionCount"),
            mainContext.select(
                DSL.count(TRANSACTION_ITEM.ID)
            ).from(IMAGE)
                .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
                .innerJoin(TRANSACTION_ITEM).on(itemToItemImagesPath)
                .innerJoin(TRANSACTION).on(itemToTransactionPath) //TODO Candidate for reworking into a single join clause
                .where(IMAGE.PROFILE_ID.eq(profileId)).asField<Long>("itemCount")
        )
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(ImageWithStats::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getOverallStats(profileId: Long): ImageDocumentStats {
        val selectStep = mainContext.select(
            DSL.count(IMAGE.ID).`as`("count"),
            DSL.ifnull(DSL.sum(IMAGE.SIZE_BYTES), 0L).`as`("totalSizeBytes")
        )
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(ImageDocumentStats::class.java)!!
    }

    fun getImageStats(profileId: Long, id: Long): TransactionAndItemCount {
        val selectStep = mainContext.select(
            DSL.countDistinct(TRANSACTION.ID).`as`("transactionCount"),
            DSL.count(TRANSACTION_ITEM.ID).`as`("itemCount")
        ).from(IMAGE)
            .innerJoin(TRANSACTION_ITEM_IMAGES).on(imageToItemImagesPath)
            .innerJoin(TRANSACTION_ITEM).on(itemToItemImagesPath)
            .innerJoin(TRANSACTION).on(itemToTransactionPath) //TODO Candidate for reworking into a single join clause
            .where(IMAGE.ID.eq(id))
            .and(IMAGE.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(TransactionAndItemCount::class.java)!!
    }


    fun getMaxOrdinal(profileId: Long): Short? {
        return getMaxOrdinalTrx(mainContext, profileId)
    }

    fun getMaxOrdinalTrx(trxContext: DSLContext, profileId: Long): Short? {
        val selectStep = trxContext.select(
            DSL.max(IMAGE.ORDINAL)
        ).from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.value1()
    }

    fun getAllByIds(profileId: Long, ids: List<Long>): List<ImagePojo> {
        val selectStep = mainContext.select(IMAGE.fields().toList())
            .from(IMAGE)
            .where(IMAGE.PROFILE_ID.eq(profileId))
            .and(IMAGE.ID.`in`(ids))

        val result = selectStep.fetch().into(ImagePojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }



    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageRepository::class.java)
    }
}