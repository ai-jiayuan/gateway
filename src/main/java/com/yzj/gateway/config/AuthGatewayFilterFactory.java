package com.yzj.gateway.config;

import com.uniccc.client.service.IAuthService;
import com.uniccc.gateway.filter.AccessGatewayFilter;
import com.yzj.gateway.filter.AccessGatewayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    @Autowired
    private IAuthService authService;

    @Override
    public GatewayFilter apply(Object config) {
        return new AccessGatewayFilter(authService);
    }
}
