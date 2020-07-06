package com.hk.voting.services

import com.hk.voting.utility.CacheKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisKeyValueTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant


@Service
class RedisService @Autowired constructor(
        private val redis: StringRedisTemplate
) {


    fun get(key: String): String? {
        val ops = redis.opsForValue()
        return ops.get(key)
    }


    fun set(key: String, value: String, instant: Instant? = null) {
        val ops = redis.opsForValue()
        ops.set(key, value)
        if (instant != null) {
            redis.expireAt(key, instant)
        }
    }

    fun del(key: String) {
        redis.delete(key)
    }


    fun setex(key: String, value: String, duration: Duration? = null) {
        val ops = redis.opsForValue()
        ops.set(key, value)
        if (duration != null) {
            redis.expire(key, duration)
        }
    }


    /**
     * Atomic increment
     */
    fun inc(key: String, field: String, delta: Long): Long {
        val ops = redis.opsForHash<String, String>()
        return ops.increment(key, field, delta)
    }


    fun hGet(key: String, field: String): String? {
        val ops = redis.opsForHash<String, String>()
        return ops.get(key, field)
    }

    fun hGetAll(key: String): MutableMap<String, String>? {
        val ops = redis.boundHashOps<String, String>(key)
        return ops.entries()
    }


    fun produce(value: String) {
        val ops = redis.opsForList()
        ops.leftPush(CacheKey.MQ_TOPIC, value)
    }

    fun consume(timeout: Long): String? {
        val ops = redis.opsForList()
        return ops.rightPop(CacheKey.MQ_TOPIC, Duration.ofSeconds(timeout))
    }

}
