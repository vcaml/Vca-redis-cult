package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session
        HttpSession session = request.getSession();

        Object user = session.getAttribute("user");

        log.debug(" 0070 进入拦截器 正在提取session中的user");

        //获取session中的用户
        if(user == null){
            response.setStatus(401);
            return false;
        }

        log.debug(" 0071 拦截器检测session中用户存在 放行");

        //存在保护用户信息到threadlocal
        UserHolder.saveUser((UserDTO) user);

        //放行
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserHolder.removeUser();
    }
}
