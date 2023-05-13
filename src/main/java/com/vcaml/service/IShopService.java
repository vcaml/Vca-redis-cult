package com.vcaml.service;

import com.vcaml.dto.Result;
import com.vcaml.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author larszhang
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);
}
