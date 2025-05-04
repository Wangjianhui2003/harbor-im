package com.jianhui.project.harbor.platform.mapper;

import com.jianhui.project.harbor.platform.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
* @author wjh2
* @description 针对表【t_user(用户)】的数据库操作Mapper
* @createDate 2025-05-04 18:25:07
* @Entity generator.domain.User
*/
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from t_user where username = #{username}")
    User getByUsername(String username);
}




