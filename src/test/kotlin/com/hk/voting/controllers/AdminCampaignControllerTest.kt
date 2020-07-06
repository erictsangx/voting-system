package com.hk.voting.controllers

import com.hk.voting.models.*
import com.hk.voting.services.CampaignRepo
import com.hk.voting.services.AuthUserRepo
import com.hk.voting.services.JwtService
import com.hk.voting.test.*
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(*[AdminCampaignController::class, JwtService::class, ExceptionAdviceController::class], properties = ["embedded.containers.enabled=false"])
@TestPropertySource(locations = ["classpath:application.test.properties"])
class AdminCampaignControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @MockBean
    lateinit var repo: CampaignRepo

    @MockBean
    lateinit var userRepo: AuthUserRepo

    @MockBean
    lateinit var jwtService: JwtService

    @Test
    fun `create a campaign`() {

        //Mock auth
        method(jwtService.verify("")) {
            AuthUser(randomId, randomStr, randomStr, listOf(Role.ADMIN))
        }

        //invalid time range
        val invalid = mvc.post("/admin/api/v1/campaign/create",
                Campaign(randomStr, randomFuture, randomPast, (1..3).map { Candidate(randomStr, randomId) }))
                .andExpect(status.is4xxClientError)
                .andReturn().response.contentAsString
        assertEquals("""{"result":null,"code":422,"message":"startTime > endTime"}""", invalid)


        val expected = Campaign(randomStr, randomPast, randomFuture, (1..3).map { Candidate(randomStr, randomId) }, randomId)

        mvc.post("/admin/api/v1/campaign/create", expected)
                .andExpect(status.isOk)
                .andReturn().response.contentAsString


        argumentCaptor<Campaign>().apply {
            Mockito.verify(repo).create(capture())
            assertEquals(1, allValues.size)

            //sanitize ids
            assertNotEquals(expected.id, firstValue.id)

            assertEquals(expected.title, firstValue.title)
            assertEquals(expected.startTime, firstValue.startTime)
            assertEquals(expected.endTime, firstValue.endTime)
            assertEquals(expected.candidates.size, firstValue.candidates.size)

            //sanitize ids
            expected.candidates.forEachIndexed { idx, candidate ->
                assertNotEquals(candidate.id, firstValue.candidates[idx].id)
                assertEquals(candidate.name, firstValue.candidates[idx].name)
            }
        }
    }

    @Test
    @WithMockUser
    fun `count all votes and update counting`() {

        //Mock auth
        method(jwtService.verify("")) {
            AuthUser(randomId, randomStr, randomStr, listOf(Role.ADMIN))
        }

        val expected = Campaign(randomStr, randomPast, randomFuture, (1..3).map { Candidate(randomStr, randomId) }, randomId)
        val count = randomLong

        //setup query
        method(repo.singleOrNull(expected.id!!)) {
            expected
        }
        expected.candidates.forEach {
            method(repo.countVote(it.id)) {
                count
            }
        }

        val response = mvc.get("/admin/api/v1/campaign/confirm-vote-count", mapOf("campaignId" to expected.id!!))
                .andExpect(status.isOk)
                .andReturn().response.contentAsString
        assertEquals(emptyResponse, response)

        argumentCaptor<VoteCount>().apply {
            Mockito.verify(repo, times(3)).upsertVoteCount(capture())
            assertEquals(3, allValues.size)
            assertEquals(expected.candidates[0].id, firstValue.candidateId)
            assertEquals(count, firstValue.count)

            assertEquals(expected.candidates[1].id, secondValue.candidateId)
            assertEquals(count, secondValue.count)

            assertEquals(expected.candidates[2].id, thirdValue.candidateId)
            assertEquals(count, thirdValue.count)
        }


    }
}
