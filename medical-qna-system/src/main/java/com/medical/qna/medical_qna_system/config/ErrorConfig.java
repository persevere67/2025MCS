package com.medical.qna.medical_qna_system.config;

import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ErrorConfig implements ErrorPageRegistrar {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/");
        ErrorPage error403Page = new ErrorPage(HttpStatus.FORBIDDEN, "/");
        ErrorPage error401Page = new ErrorPage(HttpStatus.UNAUTHORIZED, "/");
        
        registry.addErrorPages(error404Page, error403Page, error401Page);
    }
}