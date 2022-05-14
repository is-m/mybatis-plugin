package com.mut.mybatis.web.config;

import com.mut.mybatis.interceptors.PageInterceptor;
import com.mut.mybatis.interceptors.ParameterInterceptor;
import com.mut.mybatis.interceptors.ParameterProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@MapperScan(basePackages = {"com.mut.mybatis.web.dao"})
public class MybatisConfig {

    private static final Logger log = LoggerFactory.getLogger(MybatisConfig.class);


    @Bean
    public String initInterceptor(SqlSessionFactory sqlSessionFactory, ParameterProvider parameterProvider) {
        PageInterceptor pageInterceptor = new PageInterceptor();
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();

        ParameterInterceptor parameterInterceptor = new ParameterInterceptor(parameterProvider);

        log.info("mybatis add PageInterceptor");
        configuration.addInterceptor(pageInterceptor);

        log.info("mybatis add ParameterInterceptor");
        configuration.addInterceptor(parameterInterceptor);

        return "interceptors";
    }

    @Bean
    public ParameterProvider parameterProvider() {
        return new ParameterProvider() {
            @Override
            public Map<String, Object> getParameter() {
                Map<String, Object> param = new HashMap<>();
                param.put("currentTimestamp", System.currentTimeMillis());
                return param;
            }
        };
    }

}
