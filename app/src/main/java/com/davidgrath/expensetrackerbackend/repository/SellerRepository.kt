package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.OrdinalSortKeyOccupancySlot
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER_LOCATION
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SellerRepository @Autowired constructor(open var mainContext: DSLContext) {

    // region Create

    fun insertSeller(sellerPojo: SellerPojo): Long {
        return insertSellerTrx(mainContext, sellerPojo)
    }

    fun insertSellerTrx(trxContext: DSLContext, sellerPojo: SellerPojo): Long {
        val seller = trxContext.newRecord(SELLER, sellerPojo)
        seller.store()
        return seller.id!!
    }

    // endregion


    fun getByProfileIdAndId(profileId: Long, id: Long): SellerPojo? {
        return getByProfileIdAndIdTrx(mainContext, profileId, id)
    }

    fun getByProfileIdAndIdTrx(trxContext: DSLContext, profileId: Long, id: Long): SellerPojo? {
        val selectStep = trxContext.selectFrom(SELLER)
            .where(SELLER.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(SellerPojo::class.java)
    }

    fun getByProfileIdAndOrdinal(profileId: Long, ordinal: Short): SellerPojo? {
        return getByProfileIdAndOrdinalTrx(mainContext, profileId, ordinal)
    }

    fun getByProfileIdAndOrdinalTrx(trxContext: DSLContext, profileId: Long, ordinal: Short): SellerPojo? {
        val selectStep = trxContext.selectFrom(SELLER)
            .where(SELLER.ORDINAL.eq(ordinal))
            .and(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(SellerPojo::class.java)
    }

    fun getByUserIdAndId(userId: Long, id: Long): SellerPojo? {
        val selectStep = mainContext.select(SELLER.asterisk())
            .from(SELLER)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))
        return selectStep.fetchOne()?.into(SellerPojo::class.java)
    }

    fun getAllByProfileId(profileId: Long): List<SellerPojo> {
        return getAllByProfileIdTrx(mainContext, profileId)
    }

    fun getAllByProfileIdTrx(trxContext: DSLContext, profileId: Long): List<SellerPojo> {
        val selectStep = trxContext.select(SELLER.asterisk())
            .from(SELLER)
            .where(SELLER.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(SellerPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    //TODO Replace get with count queries for when I want to check limits, and also getAll with get all ID queries for when I want to check for existence by user
    fun countAll(profileId: Long): Int {
        return countAllTrx(mainContext, profileId)
    }

    fun countAllTrx(trxContext: DSLContext, profileId: Long): Int {
        val selectStep = trxContext.select(DSL.count(SELLER.ID))
            .from(SELLER)
            .where(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()!!.value1()
    }


    fun getMaxOrdinalSortKeyAndOccupancySlot(profileId: Long): OrdinalSortKeyOccupancySlot {
        return getMaxOrdinalSortKeyAndOccupancySlotTrx(mainContext, profileId)
    }

    fun getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext: DSLContext, profileId: Long): OrdinalSortKeyOccupancySlot {
        val selectStep = trxContext.select(
            DSL.max(SELLER.ORDINAL).`as`("ordinal"),
            DSL.max(SELLER.SORT_KEY).`as`("sortKey"),
            DSL.max(SELLER.OCCUPANCY_SLOT).`as`("occupancySlot"),
        ).from(SELLER)
            .where(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(OrdinalSortKeyOccupancySlot::class.java)!!
    }

    fun getByIdForUpdateTrx(trxContext: DSLContext, profileId: Long, id: Long): SellerPojo {
        val selectStep = trxContext.selectFrom(SELLER)
            .where(SELLER.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))
            .forUpdate()
        return selectStep.fetchOne()!!.into(SellerPojo::class.java)
    }

    fun updateSeller(profileId: Long, id: Long, name: String): Int {
        val updateStep = mainContext.update(SELLER)
            .set(SELLER.NAME, name)
            .where(SELLER.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))
        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }


    fun moveSortKeyTrx(trxContext: DSLContext, profileId: Long, sellerId: Long, sortKey: Short): Int {

        val updateStep = trxContext.update(SELLER)
            .set(SELLER.SORT_KEY, sortKey)
            .where(SELLER.ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }


    fun getAllOccupancySlotsTrx(trxContext: DSLContext, profileId: Long): List<Short> {
        val selectStep = trxContext.select(
            SELLER.OCCUPANCY_SLOT
        ).from(SELLER)
            .where(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetch().map { it.value1() }
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerRepository::class.java)
    }
}