package com.hk.voting.services

import com.hk.voting.models.AuthUser
import com.hk.voting.models.Role
import com.hk.voting.test.randomId
import com.hk.voting.test.randomStr
import com.hk.voting.utility.eq
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import org.springframework.test.context.TestPropertySource

/**
 * Test db query with docker containers mongo:4.2.0-bionic
 */
@DataMongoTest
@Import(AuthUserRepo::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class AuthUserRepoTest {


    @Autowired
    lateinit var repo: AuthUserRepo

    @Autowired
    lateinit var mongo: MongoTemplate


    val userA = AuthUser(randomId, randomStr, randomStr, listOf(Role.ADMIN))
    val userB = AuthUser(randomId, randomStr, randomStr, listOf(Role.ADMIN, Role.EDITOR))

    @BeforeAll
    fun setup() {
        mongo.insert(userA)
        mongo.insert(userB)
    }

    @Test
    fun singleOrNull() {
        Assertions.assertEquals(userA, repo.singleOrNull(userA.username))
        Assertions.assertEquals(userB, repo.singleOrNull(userB.username))
        Assertions.assertEquals(null, repo.singleOrNull(randomId))
    }


}
