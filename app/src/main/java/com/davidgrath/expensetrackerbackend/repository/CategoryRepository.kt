package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.views.CategoryWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Category
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.PROFILE
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.TRANSACTION_ITEM
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class CategoryRepository {

    @Autowired
    open lateinit var dslContext: DSLContext
    open var CATEGORY: Category = Category.CATEGORY

    fun insertCategory(category: CategoryPojo): Long {
        val categoryDb = dslContext.newRecord(CATEGORY, category)
        categoryDb.store()
        return categoryDb.id!!
    }


    fun findByProfileIdAndStringId(profileId: Long, stringId: String): CategoryPojo? {
        val selectStep = dslContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .and(CATEGORY.STRING_ID.eq(stringId))
        return selectStep.fetchOne()?.into(CategoryPojo::class.java)
    }

    fun findByProfileIdAndId(profileId: Long, id: Long): CategoryPojo? {
        val selectStep = dslContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
            .and(CATEGORY.ID.eq(id))
        return selectStep.fetchOne()?.into(CategoryPojo::class.java)
    }

    fun findByUserIdAndId(userId: Long, id: Long): Long? {
        val selectStep = dslContext.select(CATEGORY.ID)
            .from(CATEGORY)
            .innerJoin(PROFILE).on(PROFILE.ID.eq(CATEGORY.PROFILE_ID))
            .where(PROFILE.USER_ID.eq(userId))
            .and(CATEGORY.ID.eq(id))
        return selectStep.fetchOne()?.value1()
    }

    fun getAllByProfileId(profileId: Long): List<CategoryPojo> {
        val selectStep = dslContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
        return selectStep.fetch().into(CategoryPojo::class.java)
    }

    fun getAllByProfileIdWithStats(profileId: Long): List<CategoryWithStats> {

        val selectStep = dslContext.select(
            CATEGORY.ID, CATEGORY.PROFILE_ID, CATEGORY.STRING_ID, CATEGORY.IS_CUSTOM, CATEGORY.NAME, CATEGORY.ICON,
            dslContext.select(DSL.countDistinct(TRANSACTION.ID))
                .from(TRANSACTION)
                .innerJoin(TRANSACTION_ITEM).on(TRANSACTION.ID.eq(TRANSACTION_ITEM.ID))
                .where(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).asField<Int>("transactionCount"),
            dslContext.select(DSL.count(TRANSACTION_ITEM.ID))
                .from(TRANSACTION_ITEM)
                .where(TRANSACTION_ITEM.PRIMARY_CATEGORY_ID.eq(CATEGORY.ID)).asField<Int>("itemCount")

        )
            .from(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
        return selectStep.fetch().into(CategoryWithStats::class.java)
    }

    fun updateCategory(id: Long, profileId: Long, name: String, icon: String): Int {
        val updateStep = dslContext.update(CATEGORY)
            .set(CATEGORY.NAME, name)
            .set(CATEGORY.ICON, icon)
            .where(CATEGORY.ID.eq(id))
            .and(CATEGORY.PROFILE_ID.eq(profileId))
        return updateStep.execute()
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(CATEGORY).where("true")
        return delete.execute()
    }
}