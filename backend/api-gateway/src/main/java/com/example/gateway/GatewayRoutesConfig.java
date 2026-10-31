package com.example.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return route("user-service")
                .route(RequestPredicates.path("/auth/**"), http())
                .route(RequestPredicates.path("/me"), http())
                .route(RequestPredicates.path("/me/**"), http())
                .filter(lb("user-service"))
                .build()
                .and(route("product-service")
                        .route(RequestPredicates.path("/products"), http())
                        .route(RequestPredicates.path("/products/**"), http())
                        .filter(lb("product-service"))
                        .build())
                .and(route("media-service")
                        .route(RequestPredicates.path("/media/images"), http())
                        .route(RequestPredicates.path("/media/images/**"), http())
                        .filter(lb("media-service"))
                        .build())
                .and(route("orderservice")
                        .route(RequestPredicates.path("/cart"), http())
                        .route(RequestPredicates.path("/cart/**"), http())
                        .route(RequestPredicates.path("/orders"), http())
                        .route(RequestPredicates.path("/orders/**"), http())
                        .route(RequestPredicates.path("/seller/orders/**"), http())
                        .filter(lb("orderservice"))
                        .build());
    }
}
