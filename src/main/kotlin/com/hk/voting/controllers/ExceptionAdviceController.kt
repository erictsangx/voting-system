package com.hk.voting.controllers


import com.hk.voting.exceptions.BaseException
import com.hk.voting.models.ResponseWrapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.servlet.http.HttpServletResponse


/**
 * Catch all exceptions here
 */
@RestControllerAdvice
class ExceptionAdviceController {

    companion object {
        const val Forbidden = "Forbidden"
        const val INTERNAL_SERVER_ERROR = "Internal Server Error"
        const val BAD_JSON = "JSON parse error"
    }

    private val logger = LoggerFactory.getLogger(ExceptionAdviceController::class.java)


    /**
     * Invalid JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonException(response: HttpServletResponse, ex: HttpMessageNotReadableException): ResponseWrapper {
        logger.info(ex.localizedMessage)
        val code = HttpStatus.BAD_REQUEST.value()
        response.status = code
        return ResponseWrapper(null, code, BAD_JSON)
    }

    /**
     * Secured[ROLE_*] insufficient authority
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleAccessDeniedException(response: HttpServletResponse, ex: org.springframework.security.access.AccessDeniedException): ResponseWrapper {
        logger.info(ex.localizedMessage)
        val code = HttpStatus.FORBIDDEN.value()
        response.status = code
        return ResponseWrapper(null, code, Forbidden)
    }

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(response: HttpServletResponse, ex: BaseException): ResponseWrapper {
        logger.info(ex.localizedMessage)
        val code = ex.status.value()
        response.status = code
        return ResponseWrapper(null, code, ex.message)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllException(response: HttpServletResponse, ex: Exception): ResponseWrapper {
        logger.error("Handle Exception:", ex)
        val code = HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.status = code
        return ResponseWrapper(null, code, INTERNAL_SERVER_ERROR)
    }
}
