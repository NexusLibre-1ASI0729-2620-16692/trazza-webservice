package com.trazza.trazzawebservice.iam.infrastructure.authorization;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Registers the authorization interceptor and the {@link AuthenticatedUser} argument resolver. */
@Configuration
public class AuthorizationConfiguration implements WebMvcConfigurer {

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthenticatedUser.class);
    }

    private final AuthorizationInterceptor authorizationInterceptor;
    private final AuthenticatedUserArgumentResolver argumentResolver;

    public AuthorizationConfiguration(AuthorizationInterceptor authorizationInterceptor,
                                      AuthenticatedUserArgumentResolver argumentResolver) {
        this.authorizationInterceptor = authorizationInterceptor;
        this.argumentResolver = argumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(argumentResolver);
    }
}
