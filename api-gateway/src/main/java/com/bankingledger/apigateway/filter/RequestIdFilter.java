package com.bankingledger.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

@Component
@Slf4j
public class RequestIdFilter extends AbstractGatewayFilterFactory<RequestIdFilter.Config> {

    public RequestIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            if (!request.getHeaders().containsKey("X-Request-ID")) {
                ServerHttpRequest newRequest = request.mutate()
                        .header("X-Request-ID", UUID.randomUUID().toString())
                        .build();
                
                ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                log.debug("Added X-Request-ID header to request");
                return chain.filter(newExchange);
            }
            
            return chain.filter(exchange);
        };
    }

    public static class Config {
    }
}
