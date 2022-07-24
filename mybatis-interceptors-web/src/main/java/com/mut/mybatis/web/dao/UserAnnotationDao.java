package com.mut.mybatis.web.dao;

import com.mut.mybatis.interceptors.vo.PageVO;
import com.mut.mybatis.interceptors.vo.PagedResult;
import com.mut.mybatis.web.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface UserAnnotationDao {

    @Select("select * from userinfo")
    // @ResultType(UserVO.class)
    PagedResult<UserVO> findUserMapPage(@Param("page") PageVO page);
}
