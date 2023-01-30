package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCommand;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        // Shop shop = queryWithPassThrough(id);
        Shop shop = cacheClient
                .queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, 10L, TimeUnit.SECONDS);
        // 逻辑过期解决缓存击穿
        // Shop shop = queryWithLogicalExpire(id);
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }
        // 7. 返回
        return Result.ok(shop);
    }

/*    private Shop queryWithMutex(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 2. 判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            // 3. 存在, 返回商铺信息
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 不为null, 上面又没走, 只可能是因为之前查询之后发现是空值, 将""存入了redis中
        if(shopJson != null){
            return null;
        }

        // 4. 实现缓存重建
        // 4.1. 获取互斥锁
        String shopLockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(shopLockKey);
            // 4.2. 判断是否获取成功
            if(!isLock){
                // 4.3. 失败, 则休眠并充实
                Thread.sleep(5);
                return queryWithMutex(id);
            }
            // 4.4. 成功, 根据id查询数据库
            shop = getById(id);
            // 模拟重建的延时
            Thread.sleep(200);
            // 5. 不存在, 返回错误
            if (shop == null){
                // 将空值写入缓存
                stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 6. 存在, 写入redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7. 释放互斥锁
            unlock(shopLockKey);
        }
        // 8. 返回
        return shop;
    }*/

/*    // 使用线程池开启线程
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public Shop queryWithLogicalExpire(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 2. 判断是否存在
        if(StrUtil.isBlank(shopJson)){
            // 3. 不存在,
            return null;
        }
        // 4. 命中, 需要先将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            // 5.1. 未过期, 直接返回商铺信息
            return shop;
        }
        // 5.2. 过期, 需要缓存重建

        // 6. 缓存重建
        // 6.1. 获取互斥锁
        String shopLockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(shopLockKey);
        // 6.2. 判断是否获取锁成功
        if (isLock) {
            // 6.3. 成功, 开启独立线程实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重新保存到redis中
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(shopLockKey);
                }
            });
        }
        // 6.4. 获取锁成功失败都返回过期的商铺信息, 因为此时数据还没更新
        return shop;
    }*/

/*
    public Shop queryWithPassThrough(Long id) {
        String shopKey = RedisConstants.CACHE_SHOP_KEY + id;
        // 1. 从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        // 2. 判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            // 3. 存在, 返回商铺信息
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 不为null, 上面又没走, 只可能是因为之前查询之后发现是空值, 将""存入了redis中
        if(shopJson != null){
            return null;
        }
        // 4. 不存在, 根据id查询数据库
        Shop shop = getById(id);
        // 5. 不存在, 返回错误
        if (shop == null){
            // 将空值写入缓存
            stringRedisTemplate.opsForValue().set(shopKey, "", RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6. 存在, 写入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7. 返回
        return shop;
    }
*/

/*    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        // 1. 查询店铺数据
        Shop shop = getById(id);
        // 模拟缓存重建延时
        Thread.sleep(200);
        // 2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 写入Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }*/

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        // 1. 更新数据库
        updateById(shop);
        // 2. 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1. 不需要去redis查坐标, 则直接走数据库
        if (x == null || y == null) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        // 2. 查询的数据从第几条开始到第几条结束
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3. 查找redis中的店铺并分页, geosearch会自动升序排序
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        // geosearch key bylonlat x y byradius 10 withdistance
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        // 只能从0查到end再手动切割
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end)
                );
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        // 4. 解析店铺id
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        // 虽然list一定非空, 但截取了之后就不一定了, 所以还得判断一下截取之后的集合是否为空.
        // skip(0)表示跳到第0个元素开始取, 包括第0个.
        // TODO 测试不加等号
        if (list.size() <= from) {
            return Result.ok(Collections.emptyList());
        }

        // 5. redis中查出来的距离存入map<店铺id, 距离>中, 方便后续设置距离
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().forEach(result -> {
            // 查店铺id
            String shopIdStr = result.getContent().getName();
            // 加入ids用以拼接
            ids.add(Long.valueOf(shopIdStr));
            // 加入距离到map
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });

        // 6. 根据id查商铺, 并设置各自的距离
        String idStr = StrUtil.join(", ", ids);
        // 按照拼接id的顺序查询商铺
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
           shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        Map<String, String> map = new HashMap();
        return Result.ok(shops);
    }
}
