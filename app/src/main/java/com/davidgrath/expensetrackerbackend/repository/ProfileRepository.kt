package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Profile
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
open class ProfileRepository {

    @Autowired
    open lateinit var dslContext: DSLContext
    open var PROFILE: Profile = Profile.PROFILE

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

    fun findAllByUserId(userId: Long): List<ProfilePojo> {
        val selectStep = dslContext.selectFrom(PROFILE)
            .where(PROFILE.USER_ID.eq(userId))
            .limit(DEFAULT_FETCH_LIMIT)
        val result = selectStep.fetch().into(ProfilePojo::class.java)
        LOGGER.info("Result size: {}", result.size)
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ProfileRepository::class.java)
    }
}