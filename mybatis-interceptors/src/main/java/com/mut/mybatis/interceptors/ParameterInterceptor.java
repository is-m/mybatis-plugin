package com.mut.mybatis.interceptors;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * 参数补充
 * 
 * 应用场景：例如在多用户或者多租户系统时，我们需要获取当前访问的用户或者租户
 * @author Administrator
 *
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "parameterize", args = {java.sql.Statement.class})})
public class ParameterInterceptor implements Interceptor {

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    System.out.println("intercept...intercept:" + invocation.getMethod());
    // 动态的改变一下sql运行的参数：以前1号员工，实际从数据库查询3号员工
    Object target = invocation.getTarget();
    System.out.println("当前拦截到的对象：" + target);
    // 拿到：StatementHandler==>ParameterHandler===>parameterObject
    // 拿到target的元数据
    MetaObject metaObject = SystemMetaObject.forObject(target);
    Object value = metaObject.getValue("parameterHandler.parameterObject");
    System.out.println("sql语句用的参数是：" + value);
    // 修改完sql语句要用的参数
    metaObject.setValue("parameterHandler.parameterObject", 3);
    // metaObject.getValue() 可以取到拦截目标对象 StatementHandler
    // 里面的属性；在BaseStatementHandler里看StatementHandler所有可以取到属性
    Object mappedStatement = metaObject.getValue("parameterHandler.mappedStatement");

    System.out.println("mappedStatement：" + mappedStatement);
    // 执行目标方法
    return invocation.proceed();
  }

}
