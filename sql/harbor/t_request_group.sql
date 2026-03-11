create table t_request_group
(
    id              bigint auto_increment
        primary key,
    group_id        bigint       not null comment '群id',
    request_user_id bigint       not null comment '请求人id',
    status          tinyint      not null comment '处理状态(枚举:未处理0，同意1，拒绝2)',
    request_note    varchar(255) null comment '请求留言',
    deal_user_id    bigint       null comment '处理人id',
    comment         varchar(255) null comment '处理结果原因，理由',
    deal_time       datetime     null comment '处理日期',
    created_time    datetime     not null comment '创建日期',
    update_time     datetime     null comment '更新时间'
)
    comment '加入群请求表' collate = utf8mb4_0900_ai_ci
                           row_format = DYNAMIC;

create index idx_group_status
    on t_request_group (group_id, status)
    comment '优化按群查看申请';

create index idx_request_user
    on t_request_group (request_user_id)
    comment '优化按申请人查询';

