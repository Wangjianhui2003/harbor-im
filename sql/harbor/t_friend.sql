create table t_friend
(
    id                bigint                                 not null
        primary key,
    user_id           bigint                                 not null comment '用户id',
    friend_id         bigint                                 not null comment '好友id',
    friend_nickname   varchar(255)                           not null comment '好友昵称',
    remark            varchar(255)                           null comment '备注名',
    friend_head_image varchar(255) default ''                null comment '好友头像',
    deleted           tinyint                                null comment '删除标识  0：正常   1：已删除',
    update_time       datetime                               null comment '更新时间',
    created_time      datetime     default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '好友' collate = utf8mb4_0900_ai_ci
                   row_format = DYNAMIC;

create index idx_friend_id
    on t_friend (friend_id);

create index idx_user_id
    on t_friend (user_id);

create index idx_user_friend_deleted
    on t_friend (user_id, friend_id, deleted);
