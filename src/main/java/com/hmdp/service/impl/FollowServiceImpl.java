package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1. 拿到用户
        Long userId = UserHolder.getUser().getId();
        // 哪个用户
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
        // 2. 根据前端传回的是要进行关注还是取关执行相关操作
        if (isFollow) {
            // 2. 关注, 则往tb_follow表中新增数据
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 关注了哪些人, 将其加入redis
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        }else {
            // 3. 取关, 删除tb_follow表中相关数据 delete from tb_follow where user_id = ? and follow_user_id = ?
            Boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 从redis中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // tb_follow表中有数据就是关注了
        // 1. 还是先获取登录用户id
        Long userId = UserHolder.getUser().getId();
        // 2. 查询是否在表中(即count > 0)
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 3. 返回前端0或1进行提示
        return Result.ok(count > 0);
    }

    /**
     *
     * @param id 要查看的用户的id
     * @return
     */
    @Override
    public Result followCommons(Long id) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 求交集
        String key = RedisConstants.FOLLOW_USER_KEY + id;
        String key2 = RedisConstants.FOLLOW_USER_KEY + userId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        // 判空
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 3. 得到的id交集将String解析成Long
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4. 根据ids查users并封装成userDTO
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

}
