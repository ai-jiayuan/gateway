package com.yzj.gateway.gateway.exception;


import com.uniccc.common.core.entity.vo.ResultDto;
import com.uniccc.common.core.exception.BaseException;
import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import static com.uniccc.common.core.enums.ResultCode.*;

@Slf4j
@Component
public class GateWayExceptionHandlerAdvice {

    @ExceptionHandler(value = {ResponseStatusException.class})
    public ResultDto handle(ResponseStatusException ex) {
        log.error("response status exception:{}", ex.getMessage());
        return ResultDto.failure(GATEWAY_ERROR);
    }

    @ExceptionHandler(value = {ConnectTimeoutException.class})
    public ResultDto handle(ConnectTimeoutException ex) {
        log.error("connect timeout exception:{}", ex.getMessage());
        return ResultDto.failure(GATEWAY_CONNECT_TIME_OUT);
    }

    @ExceptionHandler(value = {NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultDto handle(NotFoundException ex) {
        log.error("not found exception:{}", ex.getMessage());
        return ResultDto.failure(GATEWAY_NOT_FOUND_SERVICE);
    }

    @ExceptionHandler(value = {BaseException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultDto handle(BaseException ex) {
        log.error("base exception:{}", ex.getMessage());
        return ResultDto.failure(ex);
    }

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultDto handle(RuntimeException ex) {
        log.error("runtime exception:{}", ex.getMessage());
        return ResultDto.failure();
    }

    @ExceptionHandler(value = {Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultDto handle(Exception ex) {
        log.error("exception:{}", ex.getMessage());
        return ResultDto.failure();
    }

    @ExceptionHandler(value = {Throwable.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultDto handle(Throwable throwable) {
        ResultDto result = ResultDto.failure();
        if (throwable instanceof ResponseStatusException) {
            result = handle((ResponseStatusException) throwable);
        } else if (throwable instanceof ConnectTimeoutException) {
            result = handle((ConnectTimeoutException) throwable);
        } else if (throwable instanceof BaseException) {
            result = handle((BaseException) throwable);
        }else if (throwable instanceof RuntimeException) {
            result = handle((RuntimeException) throwable);
        } else if (throwable instanceof Exception) {
            result = handle((Exception) throwable);
        }
        return result;
    }
}
