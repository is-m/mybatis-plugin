package com.mut.mybatis.web.controller;

import com.mut.mybatis.interceptors.vo.PageVO;
import com.mut.mybatis.interceptors.vo.PagedResult;
import com.mut.mybatis.web.UserVO;
import com.mut.mybatis.web.dao.UserAnnotationDao;
import com.mut.mybatis.web.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/anno")
public class UserAnnotationController {
    @Autowired
    private UserAnnotationDao userDao;

    @GetMapping("/page/{pageSize}/{curPage}")
    public PagedResult<UserVO> findUser(
            @PathVariable int pageSize, @PathVariable int curPage) {
        return userDao.findUserMapPage(new PageVO(curPage, pageSize));
    }
}
