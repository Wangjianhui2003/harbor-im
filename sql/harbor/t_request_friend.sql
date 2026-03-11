create table t_request_friend
(
    id              bigint       not null
        primary key,
    request_user_id bigint       not null comment '发起请求用户id',
    receive_user_id bigint       not null comment '接收id',
    status          tinyint      not null comment '处理状态（枚举：处理中:0 同意:1拒绝:2）',
    request_note    varchar(255) null comment '请求留言',
    deal_time       datetime     null comment '处理时间',
    comment         varchar(255) null comment '回复',
    created_time    datetime     not null comment '创建日期',
    update_time     datetime     null comment '更新日期'
)
    comment '好友请求表' collate = utf8mb4_0900_ai_ci
                         row_format = DYNAMIC;

create index idx_receive_status
    on t_request_friend (receive_user_id, status)
    comment '优化“我收到的请求”查询';

create index idx_request_receive
    on t_request_friend (request_user_id, receive_user_id)
    comment '优化“我发出的请求”查询，精准查找特定的发送-接收关系';

