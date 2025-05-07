package com.jianhui.project.harbor.platform.mapper;

import com.jianhui.project.harbor.platform.entity.Group;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

public interface GroupMapper extends BaseMapper<Group> {

    @Update("update harbor.t_group_member set user_nickname = #{nickname}, head_image = #{headImageThumb} where user_id = #{id}")
    void updateMemberNicknameAndThumb(Long id, String nickname, String headImageThumb);

}




