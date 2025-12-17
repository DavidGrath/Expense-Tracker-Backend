package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Category
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.jooq.DSLContext
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

    fun getAllByProfileId(profileId: Long): List<CategoryPojo> {
        val selectStep = dslContext.selectFrom(CATEGORY)
            .where(CATEGORY.PROFILE_ID.eq(profileId))
        return selectStep.fetch().into(CategoryPojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(CATEGORY).where("true")
        return delete.execute()
    }
}