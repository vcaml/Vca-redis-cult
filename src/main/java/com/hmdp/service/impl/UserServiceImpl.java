package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
          //校验手机号   //手机号不符合 返回错误信息
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
          //符合 生成验证码 使用百宝箱hutool
        String code = RandomUtil.randomNumbers(6);
         //保存到session
         session.setAttribute("code",code);
         //发送 这里采用伪发送 把验证码打到日志
         log.debug("发送验证码成功 验证码:{}",code);

        //返回结果对象
        return Result.ok();

    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //用户已经收到了发送的验证码， 现在开始 提交 手机号和验证码
        //校验手机号
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式错误");
        }
        //校验验证码
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();

        //这里的cachecode是发验证码的时候 存在session当中的。
        //在用户没有完全关闭会话或者没有超过超时时间的情况下，这个数据会一直保存在session 所以这里把他取出来和用户输入的验证码做比对
        if(cacheCode == null || !cacheCode.toString().equals(code)){

            //不一致 直接报错
            return Result.fail("验证码错误 或者验证码已经过期");
        }

        //一致，手机号是否存在
       User user = query().eq("phone",loginForm.getPhone()).one();

        //判断用户是否存在 不存在 新用户 创建一个新用户 保存到数据库
        if (user==null){
         user = createUserWithPhone(loginForm.getPhone());
        }

        //存在 正常往下走
        //不管存不存在  要保存用户的信息到session中
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName("sc2_"+RandomUtil.randomString(8));

        //保存
        save(user);
        log.debug("保存了一个新用户：{}",user.getNickName());
        return user;
    }
}
