# mybatis-plugin

简易版分页插件，目前暂时不支持基于注解的查询分页

未提供按最大ID去获取后续数据的查询优化

示例地址，返回PagedResult<List<Map>> 使用用户自定义Count
http://localhost:8080/user/page/2/2

返回PagedResult<UserVO> 自动计算Count
http://localhost:8080/user/page2/1/1?name=a
  
返回PagedResult<UserVO> 无排序 Count
http://localhost:8080/user/page3/1/1?name=a
