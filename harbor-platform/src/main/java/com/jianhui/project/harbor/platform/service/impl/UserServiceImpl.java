package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.User;
import com.jianhui.project.harbor.platform.service.UserService;
import com.jianhui.project.harbor.platform.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author wjh2
* @description 针对表【t_user(用户)】的数据库操作Service实现
* @createDate 2025-05-04 18:25:07
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




