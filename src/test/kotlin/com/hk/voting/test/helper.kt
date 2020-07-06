package com.hk.voting.test

import com.hk.voting.models.ResponseWrapper
import com.hk.voting.utility.toJsonString
import org.bson.types.ObjectId
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom


fun <T, R> R.method(methodCall: T, callback: () -> T): OngoingStubbing<T> {
    return Mockito.`when`(methodCall).thenReturn(callback())
}

fun between(startInclusive: Instant, endExclusive: Instant): Instant {
    val startSeconds = startInclusive.epochSecond
    val endSeconds = endExclusive.epochSecond
    val random = ThreadLocalRandom
            .current()
            .nextLong(startSeconds, endSeconds)
    return Instant.ofEpochSecond(random)
}


val randomInstant: Instant
    get() {
        val tenYearsAgo = Instant.now().minus(Duration.ofDays(10 * 365))
        val tenDaysAgo = Instant.now().minus(Duration.ofDays(10))
        return between(tenYearsAgo, tenDaysAgo)
    }

val randomBool: Boolean
    get() {
        return ThreadLocalRandom.current().nextBoolean()
    }

val randomLong: Long
    get() {
        return ThreadLocalRandom.current().nextLong(0, 10000)
    }


val randomPast: Instant
    get() {
        val days = ThreadLocalRandom.current().nextLong(1, 365)
        return between(Instant.now() - Duration.ofDays(days), Instant.now())
    }

val randomFuture: Instant
    get() {
        val days = ThreadLocalRandom.current().nextLong(1, 365)
        return between(Instant.now(), Instant.now() + Duration.ofDays(days))
    }


val randomStr: String
    get() {
        return UUID.randomUUID().toString().substring(0, 8)
    }

val randomId: String
    get() {
        return ObjectId().toString()
    }


val status = MockMvcResultMatchers.status()

fun MockMvc.post(url: String, content: Any, headers: Map<String, String> = mapOf("JWT" to "")): ResultActions {
    val builder = MockMvcRequestBuilders.post(url)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content.toJsonString())
    headers.forEach {
        builder.header(it.key, it.value)
    }
    return this.perform(builder)
}

fun MockMvc.get(url: String, params: Map<String, String> = emptyMap(), headers: Map<String, String> = mapOf("JWT" to "")): ResultActions {
    val builder = MockMvcRequestBuilders.get(url).params(params.toLinkedMultiValueMap())
    headers.forEach {
        builder.header(it.key, it.value)
    }

    builder.accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
    return this.perform(builder)
}


fun Map<String, String>.toLinkedMultiValueMap(): LinkedMultiValueMap<String, String> {
    val linked = LinkedMultiValueMap<String, String>()
    this.forEach {
        linked[it.key] = it.value
    }
    return linked
}

val Any.wrapper: String
    get() = ResponseWrapper(this, 200, "").toJsonString()

val emptyResponse = ResponseWrapper(null, 200, "").toJsonString()


data class Foo(
        val id: Long,
        val str: String,
        val time: Instant
)
