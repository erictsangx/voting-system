package com.hk.voting

import com.hk.voting.controllers.CampaignController
import com.hk.voting.models.AuthUser
import com.hk.voting.models.Role
import com.hk.voting.models.VoteCount
import com.hk.voting.services.CampaignRepo
import com.hk.voting.services.JwtService
import com.hk.voting.services.RedisService
import com.hk.voting.test.*
import com.hk.voting.utility.CacheKey
import com.hk.voting.utility.eq
import com.hk.voting.utility.toData
import com.hk.voting.utility.toJsonString
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc

/**
 * Test Application setup and MQ consumer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = ["classpath:application.test.properties"])
class VotingApplicationTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mongo: MongoTemplate

    @Autowired
    lateinit var redis: StringRedisTemplate

    @Autowired
    lateinit var jwtService: JwtService


    @Autowired
    lateinit var redisService: RedisService

    @MockBean
    lateinit var campaignRepo: CampaignRepo


    @Test
    fun `Mongo and Redis connections`() {
        val foo = Foo(randomLong, randomStr, randomInstant)
        mongo.insert(foo)
        val mFoo = mongo.findOne(Query.query(Foo::id eq foo.id), Foo::class.java)
        assertEquals(foo, mFoo)

        val key = randomStr
        val ops = redis.opsForValue()
        ops.set(key, foo.toJsonString())
        val rFoo = ops.get(key)
        assertEquals(foo, rFoo!!.toData<Foo>())
    }

    @Test
    fun hello() {
        val response = mvc.get("/api/v1/test/hello")
                .andExpect(status.isOk)
                .andReturn().response.contentAsString
        val expected = """
            {"result":"hello","code":200,"message":""}
        """.trimIndent()
        assertEquals(expected, response)
    }

    @Test
    fun `data class`() {
        val response = mvc.get("/api/v1/test/foo")
                .andExpect(status.isOk)
                .andReturn().response.contentAsString
        val expected = """
            {"result":{"id":123,"str":"abc","time":"2018-05-30T21:12:15.429190Z"},"code":200,"message":""}
        """.trimIndent()
        assertEquals(expected, response)
    }

    @Test
    fun profile() {
        val response = mvc.get("/api/v1/test/profile")
                .andExpect(status.isOk)
                .andReturn().response.contentAsString
        val expected = """
            {"result":"testing-env","code":200,"message":""}
        """.trimIndent()
        assertEquals(expected, response)
    }


    @Test
    fun `jwt authentication`() {
        val unauthorized = mvc.get("/admin/api/v1/test/editor")
                .andExpect(status.is4xxClientError)
                .andReturn().response.contentAsString

        assertEquals("""{"result":null,"code":401,"message":"Unauthorized"}""", unauthorized)


        val user = AuthUser(randomId, randomStr, randomStr, listOf(Role.EDITOR))
        val token = jwtService.sign(user)!!

        val response = mvc.get("/admin/api/v1/test/editor", headers = mapOf("JWT" to token))
                .andExpect(status.isOk)
                .andReturn().response.contentAsString

        assertEquals("""{"result":"${user.username}","code":200,"message":""}""", response)
    }

    @Test
    fun `secured by role`() {
        val user = AuthUser(randomId, randomStr, randomStr, listOf(Role.EDITOR))

        val token = jwtService.sign(user)!!

        val response = mvc.get("/admin/api/v1/test/admin", headers = mapOf("JWT" to token))
                .andExpect(status.is4xxClientError)
                .andReturn().response.contentAsString
        assertEquals("""{"result":null,"code":403,"message":"Forbidden"}""", response)
    }


    @Test
    fun `MQ consumer`() {
        val voting = CampaignController.Voting(randomId, randomId, randomStr)
        val hashedVote = voting.hashed()
        redisService.produce(hashedVote.toJsonString())

        //wait for async mq
        Thread.sleep(100)
        //check duplicated vote
        Mockito.verify(campaignRepo).vote(hashedVote)
        Mockito.verify(campaignRepo, never()).upsertVoteCount(VoteCount(randomId, randomLong))


        //setup query
        Mockito.reset(campaignRepo)
        method(campaignRepo.vote(hashedVote)) {
            true
        }
        redisService.produce(hashedVote.toJsonString())
        redisService.produce(hashedVote.toJsonString())
        redisService.produce(hashedVote.toJsonString())
        Thread.sleep(100)
        //save vote
        Mockito.verify(campaignRepo, Mockito.times(3)).vote(hashedVote)

        //save vote count
        argumentCaptor<VoteCount>().apply {
            Mockito.verify(campaignRepo, Mockito.times(3)).upsertVoteCount(capture())
            assertEquals(3, allValues.size)
            assertEquals(hashedVote.candidateId, firstValue.candidateId)
            assertEquals(1, firstValue.count)

            assertEquals(hashedVote.candidateId, secondValue.candidateId)
            assertEquals(2, secondValue.count)

            assertEquals(hashedVote.candidateId, thirdValue.candidateId)
            assertEquals(3, thirdValue.count)
        }

        //check cache evict
        redisService.set(CacheKey.LIST_VOTE + hashedVote.hkId, randomStr)
        redisService.produce(hashedVote.toJsonString())
        Thread.sleep(100)
        val cache = redisService.get(CacheKey.LIST_VOTE + hashedVote.hkId)
        assertNull(cache)
    }

}
