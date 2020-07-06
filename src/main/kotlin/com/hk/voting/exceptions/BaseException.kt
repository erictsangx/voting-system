package com.hk.voting.exceptions

import org.springframework.http.HttpStatus

abstract class BaseException(override val message: String, val status: HttpStatus)
    : RuntimeException(message)


class UnprocessableException(override val message: String) : BaseException(message, HttpStatus.UNPROCESSABLE_ENTITY)

class LoginException : BaseException("Wrong Username/Password", HttpStatus.UNAUTHORIZED)
