package com.mut.mybatis.interceptors;

import com.mut.mybatis.interceptors.vo.PageVO;
import com.mut.mybatis.interceptors.vo.PagedResult;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 分页
 * <p>
 * 目前分页存在问题，1.查询使用了参数后无法
 * 插件开发详解
 * https://blog.csdn.net/inrgihc/article/details/120328307
 * SqlSource & SqlSourceBuilder
 * https://blog.csdn.net/lxlneversettle/article/details/114380640
 *
 * @author Administrator
 */
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PageInterceptor implements Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(PageInterceptor.class);
    private static final Map<String, Boolean> pageResultCheckMap = new ConcurrentHashMap<String, Boolean>();

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

        // 检查参数中是否有分页参数
        PageVO pageVO = this.getPageVO(sqlParams);
        // 如果返回值类型为分页类型，而未包含分页参数则抛出异常
        if (pageVO == null) {
            throw new IllegalArgumentException("resultType is PagedResult but condition not include PageVO at sqlid is " + currentSqlId);
        }

        long totalRecord = -1;
        if (pageVO.isIncludeTotal()) {
            // 获取总记录数，检查是否有自定义的获取总数的Sql
            String countSqlId = currentSqlId + "Count";
            boolean existsCountSqlStatement = mapperConfig.getMappedStatementNames().contains(countSqlId);
            if (existsCountSqlStatement) {
                MappedStatement countSqlStatement = mapperConfig.getMappedStatement(countSqlId);
                List<Object> list = executor.query(countSqlStatement, sqlParams, RowBounds.DEFAULT, null);
                totalRecord = resolveCount(countSqlId, list);
            } else {
                String delOrderBySql = delSqlOuterOrderByStatement(originalSql);
                String countSql = String.format("SELECT COUNT(1) FROM (%s) t", delOrderBySql);
                BoundSql newBoundSql = new BoundSql(mapperConfig, countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
                MappedStatement countMappedStatement = copyFromMappedStatement(countSqlId, mappedStatement, new BoundSqlSqlSource(newBoundSql));
                mapperConfig.addMappedStatement(countMappedStatement);
                for (ParameterMapping mapping : boundSql.getParameterMappings()) {
                    String prop = mapping.getProperty();
                    if (boundSql.hasAdditionalParameter(prop)) {
                        newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
                    }
                }
                List<Object> list = executor.query(countMappedStatement, sqlParams, RowBounds.DEFAULT, null);
                totalRecord = resolveCount(countSqlId, list);
            }

            pageVO.setTotalRecord(totalRecord);
            LOG.info("page::{}", pageVO);
        }
        List<Object> pageData = null;
        if (totalRecord > 0 || totalRecord == -1) {
            String database = prepareAndCheckDatabaseType(mapperConfig);
            String pageSql = getPagedSql(database, pageVO, originalSql);

            BoundSql newBoundSql = new BoundSql(mapperConfig, pageSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
            MappedStatement pagedMappedStatement = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));

            for (ParameterMapping mapping : boundSql.getParameterMappings()) {
                String prop = mapping.getProperty();
                if (boundSql.hasAdditionalParameter(prop)) {
                    newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
                }
            }

            pageData = executor.query(pagedMappedStatement, sqlParams, RowBounds.DEFAULT, null);
            if (totalRecord == -1) {
                totalRecord = pageData.size();
            }
        }

        PagedResult<?> result = new PagedResult<>(totalRecord, pageData);
        return Collections.singletonList(result);
    }

    private long resolveCount(String mappedStatementId, List countValueList) {
        if (countValueList == null || countValueList.isEmpty()) {
            throw new IllegalArgumentException("the count statement [" + mappedStatementId + "] execute resultList cannot be null or empty");
        }
        if (countValueList.size() > 1) {
            throw new TooManyResultsException("the count statement [" + mappedStatementId + "] execute resultList size cannot be greater than 1");
        }

        Object countValue = countValueList.get(0);
        if (countValue instanceof Long) {
            return (long) countValue;
        }
        if (countValue instanceof Integer) {
            return ((Integer) countValue).longValue();
        }
        String countType = (countValue == null ? null : countValue.getClass().getName());
        throw new IllegalArgumentException("the count statement [" + mappedStatementId + "] value type " + countType + " cannot be resolve to long value ");
    }

    private MappedStatement copyFromMappedStatement(String mappedStatementId, MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(),
                mappedStatementId,
                newSqlSource,
                ms.getSqlCommandType());

        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(ms.getKeyProperties()[0]);
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        List<ResultMap> resultMaps = ms.getResultMaps();

        // 如果statementId带有count标识，则说明是自己要创建的count语句，这里重写返回值类型为 long
        if (mappedStatementId.endsWith("Count") && resultMaps.size() > 0) {
            if (resultMaps.size() > 1) {
                throw new IllegalArgumentException("cannot support resultMaps.size > 1");
            }
            ResultMap resultMap = resultMaps.get(0);
            ResultMap newResultMap = new ResultMap.Builder(
                    ms.getConfiguration(),
                    ms.getId(),
                    Long.class,
                    resultMap.getResultMappings(),
                    resultMap.getAutoMapping()).build();
            builder.resultMaps(Collections.singletonList(newResultMap));
        }
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }

    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) throws ClassNotFoundException {
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
        // 基于注解的实现时返回类型依然是PageResult
        String currentSqlId = ms.getId();
        if (PagedResult.class.equals(ms.getResultMaps().get(0).getType())) {
            int mapperClassNameIndexEnd = currentSqlId.lastIndexOf('.');
            String mapperClass = currentSqlId.substring(0, mapperClassNameIndexEnd);
            String mapperMethod = currentSqlId.substring(mapperClassNameIndexEnd + 1);
            Class<?> clz = Class.forName(mapperClass);
            Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clz, filter -> mapperMethod.equals(filter.getName()));
            Assert.isTrue(methods.length == 1, "the sqlid " + currentSqlId + "  is not support pagable query ");
            ParameterizedType type = (ParameterizedType) TypeParameterResolver.resolveReturnType(methods[0], clz);
            Type actualTypeArgument = type.getActualTypeArguments()[0];
            ResultMap resultMap = ms.getResultMaps().get(0);
            ResultMap newReturnTypeResultMap = new ResultMap.Builder(ms.getConfiguration(), resultMap.getId(),
                    (Class<?>) actualTypeArgument, resultMap.getResultMappings()
                    , resultMap.getAutoMapping()).discriminator(resultMap.getDiscriminator()).build();
            builder.resultMaps(Collections.singletonList(newReturnTypeResultMap));
        }
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

        @Override
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

    protected String prepareAndCheckDatabaseType(Configuration configuration) throws SQLException {
        Connection connection = null;
        try {
            connection = configuration.getEnvironment().getDataSource().getConnection();
            String productName = connection.getMetaData().getDatabaseProductName();
            LOG.trace("Database productName::{} ", productName);
            productName = productName.toLowerCase();
            return productName;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
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

    // 获取方法
    private Method getMethod(String full) {
        int dotPos = full.lastIndexOf(".");
        if (dotPos <= 0) {
            throw new IllegalArgumentException("cann't get method for full info:" + full);
        }

        String fullClass = full.substring(0, dotPos);
        String methodName = full.substring(dotPos + 1);
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

    // https://blog.csdn.net/yinlongfei_love/article/details/86543107
    private String delSqlOuterOrderByStatement(String sql) {
        try {
            Select noOrderSelect = (Select) CCJSqlParserUtil.parse(sql);
            SelectBody selectBody = noOrderSelect.getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;
            List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
            if (null != orderByElements) {
                // https://blog.csdn.net/u014676619/article/details/64222347
                for (OrderByElement orderByElement : orderByElements) {
                    // 只所以不能刪除帶?问号的语句,是因为mybatis的参数占位原因
                    if (orderByElement.toString().contains("?")) {
                        LOG.warn("not support order by statement has parameter with sql {}", sql);
                        return sql;
                    }
                }
                plainSelect.setOrderByElements(null);
            }
            String result = noOrderSelect.toString();
            LOG.info("delete ordered sql is {}", result);
            return result;
        } catch (JSQLParserException e) {
            LOG.warn("Delete order by statement error with sql " + sql, e);
        }
        return sql;
    }
}
