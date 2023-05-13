package com.vcaml.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vcaml.dto.LoginFormDTO;
import com.vcaml.dto.Result;
import com.vcaml.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author larszhang
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();
}
