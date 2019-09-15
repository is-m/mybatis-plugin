package com.mut.mybatis.web.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mut.mybatis.interceptors.PageInterceptor;

@Configuration
@MapperScan(basePackages = {"com.mut.mybatis.web.dao"})
public class MybatisConfig {

  private static final Logger log = LoggerFactory.getLogger(MybatisConfig.class);


  @Bean
  public String initInterceptor(SqlSessionFactory sqlSessionFactory) {
    PageInterceptor pageInterceptor = new PageInterceptor();
    org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
    
    log.info("mybatis add PageInterceptor");
    configuration.addInterceptor(pageInterceptor);
    
    return "interceptors";
  }


}
