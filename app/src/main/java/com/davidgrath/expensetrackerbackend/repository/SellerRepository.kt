package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.SELLER
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SellerRepository @Autowired constructor(open var dslContext: DSLContext) {

    // region Create

    fun insertSeller(sellerPojo: SellerPojo): Long {
        val seller = dslContext.newRecord(SELLER, sellerPojo)
        seller.store()
        return seller.id!!
    }

    // endregion

    // region Read

    fun getByProfileIdAndId(profileId: Long, id: Long): SellerPojo? {
        val selectStep = dslContext.selectFrom(SELLER)
            .where(SELLER.ID.eq(id))
            .and(SELLER.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(SellerPojo::class.java)
    }


    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(SELLER).where("true")
        return delete.execute()
    }
}