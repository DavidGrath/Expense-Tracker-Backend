package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class UserRepository {

    @Autowired
    lateinit var dslContext: DSLContext
    val USER: User = User.USER

    fun insertUser(user: UserPojo): Long {
        val user = dslContext.newRecord(USER, user)
        user.store()
        return user.id!!
    }

    fun findById(id: Long): UserPojo? {
        val selectStep = dslContext.selectFrom(USER)
            .where(USER.ID.eq(id))
        return selectStep.fetchOne()?.into(UserPojo::class.java)
    }

    fun findByUuid(uuid: String): UserPojo? {
        val selectStep = dslContext.selectFrom(USER)
            .where(USER.UUID.eq(uuid))
        return selectStep.fetchOne()?.into(UserPojo::class.java)
    }

    fun findByUsername(username: String): UserPojo? {
        val selectStep = dslContext.selectFrom(USER)
            .where(USER.USERNAME.eq(username))
        return selectStep.fetchOne()?.into(UserPojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(USER).where("true")
        return delete.execute()
    }
}