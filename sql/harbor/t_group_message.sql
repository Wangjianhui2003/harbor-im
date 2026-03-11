create table t_group_message
(
    id            bigint                                  not null
        primary key,
    group_id      bigint                                  not null comment '群id',
    send_id       bigint                                  not null comment '发送用户id',
    send_nickname varchar(255)  default ''                null comment '发送用户昵称',
    recv_ids      varchar(1024) default ''                null comment '接收用户id,逗号分隔，为空表示发给所有成员',
    content       text                                    null comment '发送内容',
    at_user_ids   varchar(1024)                           null comment '被@的用户id列表，逗号分隔',
    receipt       tinyint       default 0                 null comment '是否回执消息',
    receipt_ok    tinyint       default 0                 null comment '回执消息是否完成',
    type          tinyint(1)                              not null comment '消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示',
    status        tinyint(1)    default 0                 null comment '状态 0:未发出  2:撤回 ',
    send_time     datetime      default CURRENT_TIMESTAMP null comment '发送时间'
)
    comment '群消息' collate = utf8mb4_0900_ai_ci
                     row_format = DYNAMIC;

create index idx_group_id
    on t_group_message (group_id);

