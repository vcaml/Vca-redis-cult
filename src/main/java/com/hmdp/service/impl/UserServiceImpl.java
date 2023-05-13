package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author larszhang
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
          //校验手机号   //手机号不符合 返回错误信息
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
          //符合 生成验证码 使用百宝箱hutool
        String code = RandomUtil.randomNumbers(6);

        //保存到session
        // session.setAttribute("code",code);
        //此处重构代码 更改原来的保存code到session中 现在用redis来存储
        //为了更好的做区分 这里的key 前面加一些前缀， 另外要加上有效期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

         //发送 这里采用伪发送 把验证码打到日志
         log.debug("发送验证码成功 验证码:{}",code);

        //返回结果对象
        return Result.ok();

    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //用户已经收到了发送的验证码， 现在开始 提交 手机号和验证码
        //校验手机号
        String phone = loginForm.getPhone();

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }

        //对验证码进行校验 是否和redis中的相同
        Object cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.toString().equals(code)){
            //校验不一致 直接报错
            return Result.fail("验证码错误 或者验证码已经过期");
        }

        //从数据库取出用户对象
        User user = query().eq("phone",loginForm.getPhone()).one();

        //判断用户是否存在
        if (user==null){
            user = createUserWithPhone(loginForm.getPhone());
        }


        //随机生成一个token 作为user对象的key
        String token = UUID.fastUUID().toString(true);

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //将user对象转换成map
        //注意这个地方有坑 因为用的是 stringRedisTemplate 所有的基础数据类型都需要是string结构。

        Map<String,Object>userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);

        //单独设置有效期
        stringRedisTemplate.expire(token,LOGIN_USER_TTL,TimeUnit.MINUTES);

        return  Result.ok(token);

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
