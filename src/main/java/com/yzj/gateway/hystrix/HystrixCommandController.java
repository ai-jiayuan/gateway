package com.yzj.gateway.hystrix;


import com.uniccc.common.core.entity.vo.ResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

import static com.uniccc.common.core.enums.ResultCode.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestController
@RequestMapping("/fallback")
public class HystrixCommandController {

    private final Logger log = LoggerFactory.getLogger(HystrixCommandController.class);

    @RequestMapping("/authorization")
    public ResponseEntity<ResultDto> authorFallback() {
        log.error("授权服务器短路");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ResultDto.failure(AUTHOR_GATEWAY_ERROR));
    }

    @RequestMapping("/authentication")
    public ResponseEntity<ResultDto> authenFallback() {
        log.error("鉴权服务器短路");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ResultDto.failure(AUTHENTIC_GATEWAY_ERROR));
    }

    @RequestMapping("/dcs")
    public ResponseEntity<ResultDto> dcsFallback() {
        log.error("采集端服务器短路");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ResultDto.failure(DCS_GATEWAY_ERROR));
    }

    @RequestMapping("/dam")
    public ResponseEntity<ResultDto> damFallback() {
        log.error("管理员后台服务器短路");
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ResultDto.failure(DAM_GATEWAY_ERROR));
    }

    @RequestMapping("/tyc")
    public ResponseEntity<ResultDto> tycFallback(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String urlMsg = Optional.ofNullable(headers.getFirst("XL-Request-url"))
                .map(url -> ";请求路径为" + url)
                .orElse("");

        String timeMsg = Optional.ofNullable(headers.getFirst("XL-start-time"))
                .map(Long::parseLong)
                .map(startTime -> System.currentTimeMillis() - startTime)
                .map(time -> ";请求时间为" + time + "毫秒")
                .orElse("");

        log.error("应用端服务器短路" + urlMsg + timeMsg);
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(TYC_GATEWAY_ERROR.getCode());
        resultDto.setMessage("页面超时，请重新刷新！");
        resultDto.setData(TYC_GATEWAY_ERROR.getMessage());
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(resultDto);
    }
}
