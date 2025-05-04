package com.jianhui.project.harbor.platform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianhui.project.harbor.platform.entity.Group;
import com.jianhui.project.harbor.platform.service.GroupService;
import com.jianhui.project.harbor.platform.mapper.GroupMapper;
import org.springframework.stereotype.Service;

/**
* @author wjh2
* @description 针对表【t_group(群)】的数据库操作Service实现
* @createDate 2025-05-04 19:42:14
*/
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group>
    implements GroupService{

}




