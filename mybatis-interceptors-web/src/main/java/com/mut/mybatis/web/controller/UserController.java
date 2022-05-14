package com.mut.mybatis.web.controller;

import java.util.List;
import java.util.Map;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mut.mybatis.interceptors.vo.PageVO;
import com.mut.mybatis.interceptors.vo.PagedResult;
import com.mut.mybatis.web.UserVO;
import com.mut.mybatis.web.dao.UserDao;

@Api
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserDao userDao;

    @GetMapping("/page/{pageSize}/{curPage}")
    public PagedResult<UserVO> findUser(
            @PathVariable int pageSize, @PathVariable int curPage) {
        return userDao.findUserMapPage(new PageVO(curPage, pageSize));
    }

    @GetMapping("/page2/{pageSize}/{curPage}")
    public PagedResult<UserVO> findUserVO(
            @RequestParam(value = "name",required = false) String name,
            @PathVariable int pageSize,
            @PathVariable int curPage) {
        return userDao.findUserVOPage(name, new PageVO(curPage, pageSize));
    }

    @GetMapping("/page3/{pageSize}/{curPage}")
    public PagedResult<UserVO> findOrderedUserPage(
            @RequestParam(value = "name",required = false) String name,
            @PathVariable int pageSize,
            @PathVariable int curPage) {
        return userDao.findOrderedUserVOPage(name, new PageVO(curPage, pageSize));
    }

}
