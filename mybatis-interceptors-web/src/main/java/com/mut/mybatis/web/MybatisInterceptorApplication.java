package com.mut.mybatis.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.oas.annotations.EnableOpenApi;

@EnableTransactionManagement
@EnableOpenApi
@SpringBootApplication
public class MybatisInterceptorApplication implements ServletContextInitializer {

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {

  }

  public static void main(String[] args) {
    ApplicationContext context = SpringApplication.run(MybatisInterceptorApplication.class, args);
    System.out.println("http://localhost:"+context.getEnvironment().getProperty("local.server.port")+"/swagger-ui/");
  }

}
