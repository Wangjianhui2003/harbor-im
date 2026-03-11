create table t_private_message
(
    id        bigint                             not null
        primary key,
    send_id   bigint                             not null comment '发送用户id',
    recv_id   bigint                             not null comment '接收用户id',
    content   text                               null comment '发送内容',
    type      tinyint(1)                         not null comment '消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示',
    status    tinyint(1)                         not null comment '状态 0:已保存 2:撤回 3:已读',
    send_time datetime default CURRENT_TIMESTAMP null comment '发送时间'
)
    comment '私聊消息' collate = utf8mb4_0900_ai_ci
                       row_format = DYNAMIC;

create index idx_recv_id
    on t_private_message (recv_id);

create index idx_send_id
    on t_private_message (send_id);
