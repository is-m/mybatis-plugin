package com.mut.mybatis.web.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;

import com.mut.mybatis.interceptors.vo.PageVO;
import com.mut.mybatis.interceptors.vo.PagedResult;
import com.mut.mybatis.web.UserVO;

// @Mapper
public interface UserDao {

  // @Select("select * from userinfo")
  // @ResultType(HashMap.class)
  public PagedResult<List<Map<String, Object>>> findUserMapPage(PageVO page);

  public PagedResult<UserVO> findUserVOPage(@Param("name") String name, @Param("page") PageVO page);
}
