package com.mut.mybatis.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;

@SpringBootApplication
public class MybatisInterceptorApplication implements ServletContextInitializer {

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {

  }

  public static void main(String[] args) {
    SpringApplication.run(MybatisInterceptorApplication.class, args);
  }

}
