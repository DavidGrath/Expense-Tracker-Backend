package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.OrdinalSortKeyOccupancySlot
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
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
class SellerLocationRepository @Autowired constructor(open var mainContext: DSLContext) {

    // region Create

    fun insertSellerLocation(sellerLocationPojo: SellerLocationPojo): Long {
        return insertSellerLocationTrx(mainContext, sellerLocationPojo)
    }

    fun insertSellerLocationTrx(trxContext: DSLContext, sellerLocationPojo: SellerLocationPojo): Long {
        val sellerLocation = trxContext.newRecord(SELLER_LOCATION, sellerLocationPojo)
        sellerLocation.store()
        return sellerLocation.id!!
    }

    // endregion

    // region Read

    fun getByProfileIdAndId(profileId: Long, id: Long): SellerLocationPojo? {
        val selectStep = mainContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .where(SELLER_LOCATION.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(SellerLocationPojo::class.java)
    }

    fun getByUserIdAndId(userId: Long, id: Long): SellerLocationPojo? {
        val selectStep = mainContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER_LOCATION.ID.eq(id))
            .and(PROFILE.USER_ID.eq(userId))
        return selectStep.fetchOne()?.into(SellerLocationPojo::class.java)
    }

    fun getByProfileIdAndSellerId(profileId: Long, sellerId: Long): List<SellerLocationPojo> {
        return getByProfileIdAndSellerIdTrx(mainContext, profileId, sellerId)
    }

    //TODO Maybe create specific (Id,SortKey) tuple classes for these
    fun getByProfileIdAndSellerIdTrx(trxContext: DSLContext, profileId: Long, sellerId: Long): List<SellerLocationPojo> {
        val selectStep = trxContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .where(SELLER.ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(SellerLocationPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun countByProfileIdAndSellerId(profileId: Long, sellerId: Long): Int {
        return countByProfileIdAndSellerIdTrx(mainContext, profileId, sellerId)
    }
    fun countByProfileIdAndSellerIdTrx(trxContext: DSLContext, profileId: Long, sellerId: Long): Int {
        val selectStep = trxContext.select(DSL.count(SELLER_LOCATION.ID))
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .where(SELLER.ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()!!.value1()
    }

    fun getMaxOrdinalForSeller(profileId: Long, sellerId: Long): Short? {
        val selectStep = mainContext.select(DSL.max(SELLER_LOCATION.ORDINAL))
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER.ID.eq(sellerId))
            .and(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.value1()
    }


    fun getMaxOrdinalSortKeyAndOccupancySlot(profileId: Long, sellerId: Long): OrdinalSortKeyOccupancySlot {
        return getMaxOrdinalSortKeyAndOccupancySlotTrx(mainContext, profileId, sellerId)
    }

    fun getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext: DSLContext, profileId: Long, sellerId: Long): OrdinalSortKeyOccupancySlot {
        val selectStep = trxContext.select(
            DSL.max(SELLER_LOCATION.ORDINAL).`as`("ordinal"),
            DSL.max(SELLER_LOCATION.SORT_KEY).`as`("sortKey"),
            DSL.max(SELLER_LOCATION.OCCUPANCY_SLOT).`as`("occupancySlot"),
        ).from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(SELLER.PROFILE_ID))
            .where(SELLER_LOCATION.PROFILE_ID.eq(profileId))
            .and(SELLER.PROFILE_ID.eq(profileId)) //TODO this is a bug

        return selectStep.fetchOne()?.into(OrdinalSortKeyOccupancySlot::class.java)!!
    }

    fun getAllByProfileId(profileId: Long): List<SellerLocationPojo> {
        val selectStep = mainContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(sellerLocationToSellerPath)
            .where(SELLER.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)

        val result = selectStep.fetch().into(SellerLocationPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getAllOccupancySlotsTrx(trxContext: DSLContext, profileId: Long): List<Short> {
        val selectStep = trxContext.select(
            SELLER_LOCATION.OCCUPANCY_SLOT
        ).from(SELLER_LOCATION)
            .where(SELLER_LOCATION.PROFILE_ID.eq(profileId))

        return selectStep.fetch().map { it.value1() }
    }

    // endregion

    // region Update

    fun updateSellerLocation(profileId: Long, id: Long, name: String): Int {
        val updateStep = mainContext.update(SELLER_LOCATION)
            .set(SELLER_LOCATION.NAME, name)
            .from(SELLER_LOCATION.innerJoin(SELLER).on(sellerLocationToSellerPath))
            .where(SELLER_LOCATION.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }


    //TODO Document this "...Trx" convention formally
    fun moveSortKeyTrx(trxContext: DSLContext, profileId: Long, categoryId: Long, sortKey: Short): Int {

        val updateStep = trxContext.update(SELLER_LOCATION)
            .set(SELLER_LOCATION.SORT_KEY, sortKey)
            .where(SELLER_LOCATION.ID.eq(categoryId))
            .and(SELLER_LOCATION.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }

    // endregion


    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerLocationRepository::class.java)
    }
}