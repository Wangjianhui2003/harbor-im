//http response
export interface HttpResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

// 用户
export interface User {
  id: number;
  username: string;
  nickname: string;
  headImage: string;
  headImageThumb: string;
  email?: string;
  phoneNumber?: string;
  sex?: number;
  isBanned?: boolean;
  signature: string;
  type: number;
}

// 好友相关类型
export interface Friend {
  id: number;
  nickname: string;
  headImage: string;
  headImageThumb: string;
  online: boolean;
  onlineWeb: boolean;
  onlineApp: boolean;
  deleted?: boolean;
}

// 群组相关类型
export interface Group {
  id: number;
  name: string;
  headImage: string;
  headImageThumb: string;
  ownerId: number;
  memberCount: number;
  notice?: string;
  remark?: string;
  quit?: boolean;
}

export interface GroupMember {
  userId: number;
  nickname: string;
  headImage: string;
  showNickname: string;
  remark: string;
  isOwner: boolean;
  quit: boolean;
}

// 消息相关类型
export interface MessageInfo {
  id: number;
  sendId: number;
  recvId: number;
  content: string;
  type: number;
  status: number;
  sendTime: number;
  sendNickname?: string;
  loadStatus?: string;
  selfSend?: boolean;
  groupId?: number;
  atUserIds?: number[];
  receipt?: boolean;
  receiptOk?: boolean;
}

// 聊天会话类型
export interface Chat {
  targetId: number;
  type: string;
  showName: string;
  headImage: string;
  lastContent: string;
  lastSendTime: number;
  unreadCount: number;
  messages: MessageInfo[];
  atMe: boolean;
  atAll: boolean;
  stored: boolean;
  delete: boolean;
}

// 聊天信息类型
export interface ChatInfo {
  targetId: number;
  type: string;
  showName: string;
  headImage: string;
}

// RTC信息类型
export interface RTCInfo {
  friend: Friend | Record<string, never>;
  mode: string;
  state: number;
}

// WebRTC配置
export interface WebRTCConfig {
  maxChannel?: number;
  iceServers?: RTCIceServer[];
}

// 登录相关类型
export interface LoginData {
  username: string;
  password: string;
  captcha: string;
  captchaId: string;
}

export interface RegisterData {
  username: string;
  password: string;
  nickname: string;
  captcha: string;
  captchaId: string;
}

// API响应类型
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

// 在线终端类型
export interface OnlineTerminal {
  userId: number;
  terminals: number[];
}

// WebSocket发送信息
export interface WsSendInfo {
  cmd: number;
  data: unknown;
}

// 私聊消息DTO
export interface PrivateMessageDTO {
  recvId: number;
  content: string;
  type: number;
  receipt?: boolean;
}

// 群聊消息DTO
export interface GroupMessageDTO {
  groupId: number;
  content: string;
  type: number;
  atUserIds?: number[];
  receipt?: boolean;
}
