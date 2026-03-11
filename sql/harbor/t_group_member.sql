create table t_group_member
(
    id                bigint                                 not null
        primary key,
    group_id          bigint                                 not null comment '群id',
    user_id           bigint                                 not null comment '用户id',
    user_nickname     varchar(255) default ''                null comment '用户昵称',
    remark_nickname   varchar(255) default ''                null comment '显示昵称备注',
    role              tinyint                                null comment '角色',
    head_image        varchar(255) default ''                null comment '用户头像',
    remark_group_name varchar(255) default ''                null comment '显示群名备注',
    quit              tinyint(1)   default 0                 null comment '是否已退出',
    quit_time         datetime                               null comment '退出时间',
    update_time       datetime                               null comment '更新时间',
    created_time      datetime     default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '群成员' collate = utf8mb4_0900_ai_ci
                     row_format = DYNAMIC;

create index idx_group_id
    on t_group_member (group_id);

create index idx_user_id
    on t_group_member (user_id);

