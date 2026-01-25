package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.USER
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class UserRepository @Autowired constructor(open var dslContext: DSLContext) {


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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(UserRepository::class.java)
    }
}