package com.yzj.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.uniccc.client.service.IAuthService;
import com.uniccc.common.core.entity.vo.ResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.uniccc.common.core.enums.ResultCode.IP_BAN;

/**
 * 请求Ip校验
 */
@Configuration
@Slf4j
public class BanIpGatewayFilter implements GlobalFilter, Ordered {

    @Autowired
    private IAuthService authService;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = authService.getRealIp(request);
        log.debug(ip);
        if (authService.isBlacklist(ip)) {
            return unauthorized(exchange);
        } else {
            return chain.filter(exchange);
        }
    }

    /**
     * 网关拒绝，返回401
     *
     * @param
     */
    private Mono<Void> unauthorized(ServerWebExchange serverWebExchange) {
        serverWebExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        DataBuffer buffer = serverWebExchange.getResponse()
                .bufferFactory().wrap(JSON.toJSONString(ResultDto.failure(IP_BAN)).getBytes(StandardCharsets.UTF_8));
        return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
