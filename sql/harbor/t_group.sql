create table t_group
(
    id               bigint                                  not null
        primary key,
    name             varchar(255)                            not null comment '群名字',
    owner_id         bigint                                  not null comment '群主id',
    head_image       varchar(255)  default ''                null comment '群头像',
    head_image_thumb varchar(255)  default ''                null comment '群头像缩略图',
    notice           varchar(1024) default ''                null comment '群公告',
    is_banned        tinyint(1)    default 0                 null comment '是否被封禁 0:否 1:是',
    reason           varchar(255)  default ''                null comment '被封禁原因',
    dissolve         tinyint(1)    default 0                 null comment '是否已解散',
    join_type        tinyint                                 null comment '加入类型 0:直接加入 1:需要管理员同意',
    update_time      datetime                                null comment '更新时间',
    created_time     datetime      default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '群' collate = utf8mb4_0900_ai_ci
                 row_format = DYNAMIC;

