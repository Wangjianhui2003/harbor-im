package com.jianhui.project.harbor.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jianhui.project.harbor.platform.entity.Group;
import com.jianhui.project.harbor.platform.pojo.vo.GroupInviteVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupMemberVO;
import com.jianhui.project.harbor.platform.pojo.vo.GroupVO;

import java.util.List;

public interface GroupService extends IService<Group> {

    /**
     * 创建新群聊
     *
     * @param vo 群聊信息
     * @return 群聊信息
     **/
    GroupVO createGroup(GroupVO vo);

    /**
     * 修改群聊信息
     *
     * @param vo 群聊信息
     * @return 群聊信息
     **/
    GroupVO modifyGroup(GroupVO vo);

    /**
     * 删除群聊
     *
     * @param groupId 群聊id
     **/
    void deleteGroup(Long groupId);

    /**
     * 退出群聊
     *
     * @param groupId 群聊id
     */
    void quitGroup(Long groupId);

    /**
     * 将用户踢出群聊
     *
     * @param groupId 群聊id
     * @param userId  用户id
     */
    void kickFromGroup(Long groupId, Long userId);

    /**
     * 查询当前用户的所有群聊
     *
     * @return 群聊信息列表
     **/
    List<GroupVO> findGroups();

    /**
     * 邀请好友进群
     *
     * @param vo 群id、好友id列表
     **/
    void invite(GroupInviteVO vo);

    /**
     * 根据id查找群聊，并进行缓存
     * 过滤解散和被封禁的群聊
     *
     * @param groupId 群聊id
     * @return 群聊实体
     */
    Group getAndCheckById(Long groupId);

    /**
     * 根据id查找群聊
     *
     * @param groupId 群聊id
     * @return 群聊vo
     */
    GroupVO findById(Long groupId);

    /**
     * 查询群成员
     *
     * @param groupId 群聊id
     * @return List<GroupMemberVO>
     **/
    List<GroupMemberVO> findGroupMembers(Long groupId);

    /**
     * 用id查询群组(专用于查询加入)
     */
    GroupVO searchById(Long groupId);
}
