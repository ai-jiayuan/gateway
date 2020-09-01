package com.yzj.gateway.filter;


import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.uniccc.client.param.TokenMsgDto;
import com.uniccc.client.service.IAuthService;
import com.uniccc.common.core.entity.vo.ResultDto;
import com.uniccc.common.core.enums.ResultCode;
import com.uniccc.gateway.enums.ErrorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static com.uniccc.common.core.enums.ResultCode.*;
import static com.uniccc.gateway.enums.ErrorState.*;

/**
 * 请求url权限校验
 */
@RequiredArgsConstructor
@Slf4j
public class AccessGatewayFilter implements GatewayFilter, Ordered {

    private static final String BEARER = "bearer";
    /**
     * 由xl-client模块提供签权的feign客户端
     */
    private final IAuthService authService;
    /**
     * 直接通过的url
     */
    private final List<String> passUrls = Lists.newArrayList("/xl-das/api/v1/hot-word/0", "/xl-das/api/v1/hot-word/1"
            , "/xl-das/api/v1/pay/wx/pay-back", "/xl-das/api/v1/pay/ali/pay-back");


    /**
     * 1.首先网关检查token是否有效，无效直接返回401，不调用签权服务
     * 2.调用签权服务器看是否对该请求有权限，有权限进入下一个filter，没有权限返回401
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders oldHeaders = request.getHeaders();
        String authentication = oldHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        log.info("Authorization:{}", authentication);
        String identity = oldHeaders.getFirst("xl-identity");
        ServerHttpRequest.Builder requestBuild = request.mutate()
                .headers(httpHeaders -> httpHeaders.remove("token"));
        String method = request.getMethodValue();
        String url = request.getPath().value();
        log.info("url:{},method:{},headers:{}", url, method, request.getHeaders());
        if (passUrls.contains(url)) {
            ServerHttpRequest newRequest = requestBuild
                    .header("token", "ROLE_ANONYMOUS")
                    .header("XL-Request-url", url)
                    .header("XL-start-time", String.valueOf(System.currentTimeMillis()))
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }
        String ip = authService.getRealIp(request);
        ErrorState state = MALICIOUS;
        if (StringUtils.isNotBlank(authentication)
                && authentication.startsWith(BEARER)) {
            state = ErrorState.getEnum(authService.isTokenValid(authentication));
            if (Objects.isNull(state)) {
                if (authService.authenticateAuth(authentication, url, method, ip)) {
                    TokenMsgDto tokenMsgDto = authService.getJwt(authentication, identity);
                    log.info("用户名:{},clientId:{},页面身份:{}", tokenMsgDto.getUsername(), tokenMsgDto.getClientId(), tokenMsgDto.getPageIdentity());
                    log.info("token:{}", tokenMsgDto.getToken());
                    if (StringUtils.isNotBlank(tokenMsgDto.getErrorMsg())) {
                        log.info("出现错误,错误原因为:{}", tokenMsgDto.getErrorMsg());
                    }
                    if ("banUser".equals(tokenMsgDto.getToken())) {
                        return banUser(exchange);
                    }
                    if ("refresh".equals(tokenMsgDto.getToken())) {
                        return refresh(exchange);
                    }
                    ServerHttpRequest newRequest = requestBuild
                            .header("token", tokenMsgDto.getToken())
                            .header("XL-Request-url", url)
                            .header("XL-start-time", String.valueOf(System.currentTimeMillis()))
                            .build();
                    if (StringUtils.isNotBlank(tokenMsgDto.getPageIdentity())
                            && StringUtils.isNotBlank(tokenMsgDto.getSystemIdentity())
                            && !tokenMsgDto.getPageIdentity().equals(tokenMsgDto.getSystemIdentity())) {
                        log.info("页面身份:{},系统身份:{}", tokenMsgDto.getPageIdentity(), tokenMsgDto.getSystemIdentity());
                        exchange.getResponse().getHeaders().add("chang-identity-name", "true");
                    }
                    return chain.filter(exchange.mutate().request(newRequest).build());
                } else {
                    state = DENIED;
                }
            }
        } else if (StringUtils.isBlank(authentication) || authentication.startsWith("Basic")) {
            if (authService.authenticateOrg(authentication, url, method, ip)) {
                ServerHttpRequest newRequest = requestBuild
                        .header("token", "ROLE_ANONYMOUS")
                        .header("XL-Request-url", url)
                        .header("XL-start-time", String.valueOf(System.currentTimeMillis()))
                        .build();
                return chain.filter(exchange.mutate().request(newRequest).build());
            } else {
                state = NOTLOGGEDIN;
            }
        }
        return unauthorized(exchange, state);
    }

    /**
     * 网关拒绝，返回401
     *
     * @param
     */
    private Mono<Void> unauthorized(ServerWebExchange serverWebExchange, ErrorState state) {
        ResultCode resultCode = MALICIOUS_VISIT;
        switch (state) {
            case DENIED:
                serverWebExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                resultCode = UNAUTHORIZED;
                break;
            case DUPLICATE:
                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                resultCode = DUPLICATE_LOGON;
                break;
            case FAILURE:
                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                resultCode = ACCESS_TOKEN_INVALID;
                break;
            case NOTLOGGEDIN:
                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                resultCode = NOT_LOGGED_IN;
                break;
            default:
                serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        }
        log.info("错误code:{}", resultCode.getCode());
        DataBuffer buffer = serverWebExchange.getResponse()
                .bufferFactory().wrap(JSON.toJSONString(ResultDto.failure(resultCode)).getBytes(StandardCharsets.UTF_8));
        return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
    }

    private Mono<Void> refresh(ServerWebExchange serverWebExchange) {
        log.info("进行刷新页面");
        serverWebExchange.getResponse().setStatusCode(HttpStatus.I_AM_A_TEAPOT);
        DataBuffer buffer = serverWebExchange.getResponse()
                .bufferFactory().wrap(JSON.toJSONString(ResultDto.failure(SUCCESS)).getBytes(StandardCharsets.UTF_8));
        return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
    }

    private Mono<Void> banUser(ServerWebExchange serverWebExchange) {
        log.info("禁止用户登录");
        serverWebExchange.getResponse().setStatusCode(HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE);
        DataBuffer buffer = serverWebExchange.getResponse()
                .bufferFactory().wrap(JSON.toJSONString(ResultDto.failure(SUCCESS)).getBytes(StandardCharsets.UTF_8));
        return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
