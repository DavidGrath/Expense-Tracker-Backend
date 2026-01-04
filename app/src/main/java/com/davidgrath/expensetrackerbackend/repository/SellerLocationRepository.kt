package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER_LOCATION
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SellerLocationRepository @Autowired constructor(open var dslContext: DSLContext) {

    // region Create

    fun insertSellerLocation(sellerLocationPojo: SellerLocationPojo): Long {
        val sellerLocation = dslContext.newRecord(SELLER_LOCATION, sellerLocationPojo)
        sellerLocation.store()
        return sellerLocation.id!!
    }

    // endregion

    // region Read

    fun getByProfileIdAndId(profileId: Long, id: Long): SellerLocationPojo? {
        val selectStep = dslContext.select(SELLER_LOCATION.asterisk())
            .from(SELLER_LOCATION)
            .innerJoin(SELLER).on(SELLER.ID.eq(SELLER_LOCATION.SELLER_ID))
            .where(SELLER_LOCATION.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(SellerLocationPojo::class.java)
    }


    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(SELLER_LOCATION).where("true")
        return delete.execute()
    }
}