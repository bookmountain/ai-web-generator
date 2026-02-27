package com.book.aiwebgenerator.service;

import com.book.aiwebgenerator.model.dto.UserQueryRequest;
import com.book.aiwebgenerator.model.vo.LoginUserVO;
import com.book.aiwebgenerator.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.book.aiwebgenerator.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface UserService extends IService<User> {

    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVO getLoginUserVO(User user);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String password);
}
