package com.vcaml.service;

import com.vcaml.dto.Result;
import com.vcaml.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author larszhang
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result querHotBlog(Integer current);
}
