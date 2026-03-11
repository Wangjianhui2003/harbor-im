create table t_user
(
    id               bigint                                  not null
        primary key,
    username         varchar(255)                            not null comment '用户名',
    nickname         varchar(255)                            not null comment '用户昵称',
    head_image       varchar(255)  default ''                null comment '用户头像',
    head_image_thumb varchar(255)  default ''                null comment '用户头像缩略图',
    password         varchar(255)                            not null comment '密码',
    email            varchar(50)                             null comment '邮箱',
    phone_number     varchar(20)                             null comment '手机号',
    sex              tinyint(1)    default 0                 null comment '性别 0:男 1:女',
    is_banned        tinyint(1)    default 0                 null comment '是否被封禁 0:否 1:是',
    reason           varchar(255)  default ''                null comment '被封禁原因',
    type             smallint      default 1                 null comment '用户类型 1:普通用户 2:审核账户',
    add_type         tinyint                                 null comment '0:直接加好友 1:同意后加好友',
    signature        varchar(1024) default ''                null comment '个性签名',
    last_login_time  datetime                                null comment '最后登录时间',
    region           varchar(255)                            null comment '地区',
    update_time      datetime                                null comment '更新时间',
    created_time     datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    constraint idx_username
        unique (username)
)
    comment '用户' collate = utf8mb4_0900_ai_ci
                   row_format = DYNAMIC;

create index idx_nickname
    on t_user (nickname);

