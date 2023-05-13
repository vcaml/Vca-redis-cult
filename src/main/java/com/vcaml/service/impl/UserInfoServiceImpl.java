package com.vcaml.service.impl;

import com.vcaml.entity.UserInfo;
import com.vcaml.mapper.UserInfoMapper;
import com.vcaml.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author larszhang
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
