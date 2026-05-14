package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopTypeMapper shopTypeMapper;

    /**
     * 查询所有商户类型
     *
     * @return 商户类型列表
     */
    @Override
    public Result queryList() {
        //1.从Redis中获取商户类型列表
        String key = RedisConstants.CACHE_SHOPTYPE_KEY;
        List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);

        //2.商户类型列表存在，直接返回
        List<ShopType> typeList = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(list)){
            for (String s : list) {
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                typeList.add(shopType);
            }
            return Result.ok(typeList);
        }

        //3.商户类型列表不存在，从数据库查找商户类型列表
        typeList = query().orderByAsc("sort").list();

        //4.数据库也不存在商户类型列表，返回错误
        if(CollectionUtil.isEmpty(typeList)){
            return Result.fail("不存在商户类型");
        }

        //5.序列化商户类型列表
        for (ShopType shopType : typeList) {
            String s = JSONUtil.toJsonStr(shopType);
            list.add(s);
        }

        //6.将商户类型列表写入Redis
        stringRedisTemplate.opsForList().rightPushAll(key, list);
        stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);

        //7.返回ok
        return Result.ok(typeList);
    }
}
