package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.CategoryWithStats
import com.davidgrath.expensetrackerbackend.entities.views.OrdinalSortKeyOccupancySlot
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Category
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.ACCOUNT
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class CategoryRepository {

    @Autowired
    open lateinit var mainContext: DSLContext
    open var CATEGORY: Category = Category.CATEGORY

    fun insertCategory(category: CategoryPojo): Long {
        return insertCategoryTrx(mainContext, category)
    }
    fun insertCategoryTrx(trxContext: DSLContext, category: CategoryPojo): Long {
        val categoryDb = trxContext.newRecord(CATEGORY, category)
        categoryDb.store()
        return categoryDb.id!!
    }


    fun findByProfileIdAndStringId(profileId: Long, stringId: String): CategoryPojo? {
        return findByProfileIdAndStringIdTrx(mainContext, profileId, stringId)
    }

    fun findByProfileIdAndStringIdTrx(trxContext: DSLContext, profileId: Long, stringId: String): CategoryPojo? {
        val selectStep = trxContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .and(CATEGORY.STRING_ID.eq(stringId))
        return selectStep.fetchOne()?.into(CategoryPojo::class.java)
    }

    fun findByProfileIdAndId(profileId: Long, id: Long): CategoryPojo? {
        return findByProfileIdAndIdTrx(mainContext, profileId, id)
    }
    fun findByProfileIdAndIdTrx(trxContext: DSLContext, profileId: Long, id: Long): CategoryPojo? {
        val selectStep = trxContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .and(CATEGORY.ID.eq(id))
        return selectStep.fetchOne()?.into(CategoryPojo::class.java)
    }

    fun findByProfileIdAndOrdinal(profileId: Long, ordinal: Short): CategoryPojo? {
        val selectStep = mainContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .and(CATEGORY.ORDINAL.eq(ordinal))
        return selectStep.fetchOne()?.into(CategoryPojo::class.java)
    }

    fun findByUserIdAndId(userId: Long, id: Long): Long? {
        val selectStep = mainContext.select(CATEGORY.ID)
            .from(CATEGORY)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(CATEGORY.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .and(CATEGORY.ID.eq(id))
        return selectStep.fetchOne()?.value1()
    }

    fun getAllByProfileId(profileId: Long): List<CategoryPojo> {
        return getAllByProfileIdTrx(mainContext, profileId)
    }

    fun getAllByProfileIdTrx(trxContext: DSLContext, profileId: Long): List<CategoryPojo> {
        val selectStep = trxContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(CategoryPojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    fun getAllByProfileIdWithStats(profileId: Long): List<CategoryWithStats> {

        val selectStep = mainContext.select(
            CATEGORY.ID, CATEGORY.PROFILE_ID, CATEGORY.STRING_ID, CATEGORY.IS_CUSTOM, CATEGORY.NAME, CATEGORY.ICON,
            mainContext.select(DSL.countDistinct(TRANSACTION.ID))
                .from(TRANSACTION)
                .innerJoin(TRANSACTION_ITEM).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.ID))
                .where(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).asField<Long>("transactionCount"),
            mainContext.select(DSL.count(TRANSACTION_ITEM.ID))
                .from(TRANSACTION_ITEM)
                .where(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL.eq(CATEGORY.ORDINAL).and(TRANSACTION_ITEM.PROFILE_ID.eq(CATEGORY.PROFILE_ID))).asField<Long>("itemCount")

        )
            .from(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(CategoryWithStats::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }


    fun getMaxOrdinalSortKeyAndOccupancySlot(profileId: Long): OrdinalSortKeyOccupancySlot {
        return getMaxOrdinalSortKeyAndOccupancySlotTrx(mainContext, profileId)
    }

    fun getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext: DSLContext, profileId: Long): OrdinalSortKeyOccupancySlot {
        val selectStep = trxContext.select(
            DSL.max(CATEGORY.ORDINAL).`as`("ordinal"),
            DSL.max(CATEGORY.SORT_KEY).`as`("sortKey"),
            DSL.max(CATEGORY.OCCUPANCY_SLOT).`as`("occupancySlot"),
        ).from(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))

        return selectStep.fetchOne()?.into(OrdinalSortKeyOccupancySlot::class.java)!!
    }

    fun updateCategory(id: Long, profileId: Long, name: String, icon: String): Int {
        val updateStep = mainContext.update(CATEGORY)
            .set(CATEGORY.NAME, name)
            .set(CATEGORY.ICON, icon)
            .where(CATEGORY.ID.eq(id))
            .and(CATEGORY.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }

    fun moveSortKeyTrx(trxContext: DSLContext, profileId: Long, categoryId: Long, sortKey: Short): Int {

        val updateStep = trxContext.update(CATEGORY)
            .set(CATEGORY.SORT_KEY, sortKey)
            .where(CATEGORY.ID.eq(categoryId))
            .and(CATEGORY.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }


    fun getAllOccupancySlotsTrx(trxContext: DSLContext, profileId: Long): List<Short> {
        val selectStep = trxContext.select(
            CATEGORY.OCCUPANCY_SLOT
        ).from(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))

        return selectStep.fetch().map { it.value1() }
    }



    companion object {
        private val LOGGER = LoggerFactory.getLogger(CategoryRepository::class.java)
    }
}