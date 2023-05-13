package com.vcaml.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vcaml.dto.LoginFormDTO;
import com.vcaml.dto.Result;
import com.vcaml.dto.UserDTO;
import com.vcaml.entity.User;
import com.vcaml.mapper.UserMapper;
import com.vcaml.service.IUserService;
import com.vcaml.utils.RegexUtils;
import com.vcaml.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.vcaml.utils.RedisConstants.*;

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



    @Override
    public Result sign() {
        //此签到功能 按月保存 用户的签到情况
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        //获取日期 拼接key
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        //确定今天是当前月的第几天
        int dayOfMonth = now.getDayOfMonth();

        log.debug("0067 签到 key {}, day {}",key,dayOfMonth);
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);

        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        //获取日期 拼接key
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        //确定今天是当前月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //bifField GET 返回的是一个十进制的数字
        List<Long> bitResult = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if( bitResult==null || bitResult.isEmpty()){
          return Result.ok(0);
        }

        Long num = bitResult.get(0);

        if( num==null || num==0){
            return Result.ok(0);
        }

        int count = 0;
        while(true){
            if((num & 1)==0){
                break;
            }else{
              count++;
            }
           num >>>= 1;
        }
        return Result.ok(count);
    }

}
