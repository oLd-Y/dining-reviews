package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        String typeListKey = RedisConstants.CACHE_TYPE_LIST_KEY;
        // 1. 从redis查询缓存
        String typeListJson= stringRedisTemplate.opsForValue().get(typeListKey);
        // 2. 判断是否存在
        if(StrUtil.isNotBlank(typeListJson)){
            // 3. 存在, 返回类型信息
            List<ShopType> typeList= JSONUtil.toList(typeListJson, ShopType.class);
            return Result.ok(typeList);
        }
        // 4. 不存在, 根据id查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 5. 查询的是list, 跟存在数据库中的数据有关, 为空也直接返回
        if(typeList.isEmpty()){
            return Result.fail("查找不到类型列表");
        }
        // 6. 存在, 写入redis
        stringRedisTemplate.opsForValue().set(typeListKey, JSONUtil.toJsonStr(typeList));
        // 7. 返回
        return Result.ok(typeList);
    }
}
