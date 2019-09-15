package com.mut.mybatis.interceptors;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

/**
 * 用户自定义语句标签
 * 
 * @author Administrator
 */
public abstract class UserTagIntercetpor implements Interceptor {

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    return invocation.proceed();
  }
}
