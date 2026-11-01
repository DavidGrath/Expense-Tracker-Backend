package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.Profile
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.DEFAULT_FETCH_LIMIT
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.util.UUID

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

    fun findByUuid(userId: Long, uuid: ByteArray): ProfilePojo? {
        val selectStep = dslContext.selectFrom(PROFILE)
            .where(PROFILE.USER_ID.eq(userId))
            .and(PROFILE.UUID.eq(uuid))
        return selectStep.fetchOne()?.into(ProfilePojo::class.java)
    }

    fun findByUuid(userId: Long, uuid: String): ProfilePojo? {
        val uuidBytes = uuidToBytes(UUID.fromString(uuid))
        return findByUuid(userId, uuidBytes)
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