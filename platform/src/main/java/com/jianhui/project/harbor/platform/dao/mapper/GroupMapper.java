package com.jianhui.project.harbor.platform.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianhui.project.harbor.platform.dao.entity.Group;
import org.apache.ibatis.annotations.Update;

public interface GroupMapper extends BaseMapper<Group> {

    @Update("update t_group_member set user_nickname = #{nickname}, head_image = #{headImageThumb} where user_id = #{id}")
    void updateMemberNicknameAndThumb(Long id, String nickname, String headImageThumb);

}




