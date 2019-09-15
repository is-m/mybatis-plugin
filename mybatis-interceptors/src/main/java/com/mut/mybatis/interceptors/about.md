Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
是 Mybatis 的内部执行器，它负责调用 StatementHandler 操作数据库，并把结果集通过 ResultSetHandler 进行自动映射，另外，它还处理了二级缓存的操作。

ParameterHandler (getParameterObject, setParameters)
是 Mybatis 实现 sql 入参设置的对象。

StatementHandler (prepare, parameterize, batch, update, query)
是 Mybatis 直接和数据库执行 sql 脚本的对象，另外，它也实现了 Mybatis 的一级缓存。

ResultSetHandler (handleResultSets, handleOutputParameters)
是 Mybatis 把 ResultSet 集合映射成 POJO 的接口对象。