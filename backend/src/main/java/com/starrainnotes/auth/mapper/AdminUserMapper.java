package com.starrainnotes.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the single-admin table.
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
