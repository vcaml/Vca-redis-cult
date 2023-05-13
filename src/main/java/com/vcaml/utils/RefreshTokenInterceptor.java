package com.vcaml.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.vcaml.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;

    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取token 前端代码中会将token放进请求的header中 这里从header中取
        String token = request.getHeader("authorization");
        log.debug(" 0069 进入一级拦截器 正在获取前端的token{}",token);
        if(StrUtil.isBlank(token)){
           return true;
        }

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);

        log.debug(" 0070 进入一级拦截器 正在获取redis中的user：{}",userMap);
        //判断是否为null
        if(userMap.isEmpty()){
            return true;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);

        //存在保护用户信息到threadlocal
        UserHolder.saveUser(userDTO);

        //刷新token的有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserHolder.removeUser();
    }
}
