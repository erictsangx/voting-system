package com.hk.voting.utility

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty1

infix fun <T, R> KProperty1<T, R>.eq(target: Any): Criteria = Criteria.where(name).`is`(target)
infix fun <T, R> KProperty1<T, R>.lte(target: Any): Criteria = Criteria.where(name).lte(target)
infix fun <T, R> KProperty1<T, R>.gte(target: Any) = Criteria.where(name).gte(target)
infix fun <T, R, E> KProperty1<T, R>.inside(target: List<E>) = Criteria.where(name).`in`(target)

infix fun Criteria.and(target: Criteria) = this.andOperator(target)


fun <T, R> KProperty1<T, R>.desc() = Sort.by(Sort.Direction.DESC, name)


