import http from "./http.js";

/**
 * 添加好友
 */
export const addFriend = (friendId) => {
  return http({
    url: "/friend/add",
    method: "post",
    params: { friendId },
  });
};

/**
 * 查找所有好友
 */
export const getFriendList = () => {
  return http({
    url: "/friend/list",
    method: "get",
  });
};

/**
 * 移除好友
 */
export const removeFriend = (friendId) => {
  return http({
    url: `/friend/delete/${friendId}`,
    method: "delete",
  });
};

/**
 * 查找单个好友的信息
 */
export const findFriend = (friendId) => {
  return http({
    url: `/friend/find/${friendId}`,
    method: "get",
  });
};
