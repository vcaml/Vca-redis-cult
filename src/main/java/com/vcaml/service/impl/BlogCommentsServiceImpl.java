package com.vcaml.service.impl;

import com.vcaml.entity.BlogComments;
import com.vcaml.mapper.BlogCommentsMapper;
import com.vcaml.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author larszhang
 * @since 2021-12-22
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
