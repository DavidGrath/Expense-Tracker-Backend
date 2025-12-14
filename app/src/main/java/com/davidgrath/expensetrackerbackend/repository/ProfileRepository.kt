package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Account
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Profile
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Transaction
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.TransactionItem
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.User
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class ProfileRepository {

    @Autowired
    lateinit var dslContext: DSLContext
    val PROFILE: Profile = Profile.PROFILE

    fun insertProfile(profile: ProfilePojo): Long {
        val profileDb = dslContext.newRecord(PROFILE, profile)
        profileDb.store()
        return profileDb.id!!
    }

    fun findByStringId(userId: Long, stringId: String): ProfilePojo? {
        val selectStep = dslContext.selectFrom(PROFILE)
            .where(PROFILE.USER_ID.eq(userId))
            .and(PROFILE.STRING_ID.eq(stringId))
        return selectStep.fetchOne()?.into(ProfilePojo::class.java)
    }

    fun deleteAllForTesting(): Int {
        val delete = dslContext.deleteFrom(PROFILE).where("true")
        return delete.execute()
    }
}