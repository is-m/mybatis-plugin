package com.mut.mybatis.interceptors;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mut.mybatis.interceptors.vo.PageVO;
import com.mut.mybatis.interceptors.vo.PagedResult;


/**
 * 分页
 * 
 * 目前分页存在问题，1.查询使用了参数后无法
 * 
 * @author Administrator
 *
 */
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PageInterceptor implements Interceptor {

  private static final Logger LOG = LoggerFactory.getLogger(PageInterceptor.class);

  private Map<String, Boolean> pageResultCheckMap = new ConcurrentHashMap<String, Boolean>();

  /**
   * 任何时候，该接口返回值都应该为LIST，否则会提示类型转换的错误
   */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Executor executor = (Executor) invocation.getTarget();
    Object[] args = invocation.getArgs();
    MappedStatement mappedStatement = (MappedStatement) args[0];
    Object sqlParams = args[1];

    // 获取绑定的SQL
    BoundSql boundSql = mappedStatement.getBoundSql(sqlParams);
    String originalSql = boundSql.getSql().trim();
    // Object parameterObject = boundSql.getParameterObject();

    Configuration mapperConfig = mappedStatement.getConfiguration();
    Connection connection = mapperConfig.getEnvironment().getDataSource().getConnection();
    String currentSqlId = mappedStatement.getId();

    // 检查并设置当前执行的SQLID是否需要需要分页
    if (!pageResultCheckMap.containsKey(currentSqlId)) {
      Method execMethod = getMethod(currentSqlId);
      if (execMethod == null || !Objects.equals(PagedResult.class, execMethod.getReturnType())) {
        pageResultCheckMap.put(currentSqlId, false);
        return invocation.proceed();
      }
    } else if (!pageResultCheckMap.get(currentSqlId)) {
      return invocation.proceed();
    }


    // LOG.info("pageInterceptor sqlid:{} , sql:{}", currentSqlId, originalSql.replaceAll("\\s+", "
    // "));

    // 检查参数中是否有分页参数
    PageVO pageVO = this.getPageVO(sqlParams);
    // 如果返回值类型为分页类型，而未包含分页参数则抛出异常
    if (pageVO == null) {
      throw new IllegalArgumentException("resultType is PagedResult but condition not include PageVO at sqlid is " + currentSqlId);
    }

    // 获取总记录数，检查是否有自定义的获取总数的Sql
    String countSqlId = currentSqlId + "Count";
    boolean existsCountSqlStatement = mapperConfig.getMappedStatementNames().contains(countSqlId);
    long totalRecord = 0;
    if (existsCountSqlStatement) {
      MappedStatement countSqlStatement = mapperConfig.getMappedStatement(countSqlId);
      List<Object> list = executor.query(countSqlStatement, sqlParams, RowBounds.DEFAULT, null);
      if (list.size() > 1) {
        throw new TooManyResultsException();
      }
      totalRecord = (int) list.get(0);
    } else {
      // TODO 
      String countSql = String.format("SELECT COUNT(1) FROM (%s) t", originalSql);
      try (PreparedStatement statement = connection.prepareStatement(countSql)) {
        DefaultParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
        parameterHandler.setParameters(statement);
        ResultSet executeQuery = statement.executeQuery();
        if (executeQuery.next()) {
          totalRecord = executeQuery.getLong(1);
        } else {
          throw new IllegalArgumentException("not pageCount result");
        }
      }
    }

    pageVO.setTotalRecord(totalRecord);
    LOG.info("page::{}", pageVO.toString());

    List<Object> pageData = null;
    if (totalRecord > 0) {
      String database = prepareAndCheckDatabaseType(connection);
      String pageSql = getPagedSql(database, pageVO, originalSql);

      BoundSql newBoundSql =
          new BoundSql(mappedStatement.getConfiguration(), pageSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
      MappedStatement pagedMappedStatement = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));
      for (ParameterMapping mapping : boundSql.getParameterMappings()) {
        String prop = mapping.getProperty();
        if (boundSql.hasAdditionalParameter(prop)) {
          newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
        }
      }
      pageData = executor.query(pagedMappedStatement, sqlParams, RowBounds.DEFAULT, null);
    }

    PagedResult<?> result = new PagedResult<>(totalRecord, pageData);
    return Arrays.asList(result);
  }

  private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
    MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
    builder.resource(ms.getResource());
    builder.fetchSize(ms.getFetchSize());
    builder.statementType(ms.getStatementType());
    builder.keyGenerator(ms.getKeyGenerator());
    if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
      builder.keyProperty(ms.getKeyProperties()[0]);
    }
    builder.timeout(ms.getTimeout());
    builder.parameterMap(ms.getParameterMap());
    builder.resultMaps(ms.getResultMaps());
    builder.resultSetType(ms.getResultSetType());
    builder.cache(ms.getCache());
    builder.flushCacheRequired(ms.isFlushCacheRequired());
    builder.useCache(ms.isUseCache());
    return builder.build();
  }

  private class BoundSqlSqlSource implements SqlSource {
    private BoundSql boundSql;

    public BoundSqlSqlSource(BoundSql boundSql) {
      this.boundSql = boundSql;
    }

    public BoundSql getBoundSql(Object parameterObject) {
      return boundSql;
    }
  }

  protected String getPagedSql(String database, PageVO page, String sql) throws IllegalArgumentException {
    int start = page.getPageStartIndex();
    int end = start + page.getPageSize();
    switch (database) {
      case "oracle":
        return "select * from ( select u.*, rownum rn from (" + sql + ") u where rownum < " + end + ") where u.rn >= " + start;
      case "mysql":
        return sql + " limit " + (start - 1) + "," + page.getPageSize();
      default:
        throw new IllegalArgumentException("unsupport database is " + database);
    }
  }

  protected String prepareAndCheckDatabaseType(Connection connection) throws SQLException {
    String productName = connection.getMetaData().getDatabaseProductName();
    LOG.trace("Database productName::{} ", productName);
    productName = productName.toLowerCase();
    return productName;
  }

  private PageVO getPageVO(Object parameter) {
    if (parameter instanceof Map<?, ?>) {
      Map<?, ?> paramMap = (Map<?, ?>) parameter;
      for (Object paramItem : paramMap.values()) {
        if (paramItem instanceof PageVO) {
          return (PageVO) paramItem;
        }
      }
    } else if (parameter instanceof PageVO) {
      return (PageVO) parameter;
    } else {
      throw new IllegalArgumentException("unknow pageVO be found");
    }
    return null;
  }

  /**
   * 获取方法
   * 
   * @param full
   * @return
   * @since 2017年8月27日
   * @author Administrator
   */
  private Method getMethod(String full) {
    int dotPos = full.lastIndexOf(".");
    if (dotPos <= 0) {
      throw new IllegalArgumentException("cann't get method for full info:" + full);
    }

    String fullClass = full.substring(0, dotPos);
    String methodName = full.substring(dotPos + 1, full.length());
    try {
      Class<?> clz = Class.forName(fullClass);
      for (Method method : clz.getDeclaredMethods()) {
        if (methodName.equals(method.getName())) {
          return method;
        }
      }
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
