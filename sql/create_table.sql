create database if not exists ai_web_generator;

use ai_web_generator;



create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment 'account',
    userPassword varchar(512)                           not null comment 'password',
    userName     varchar(256)                           null comment 'user name',
    userAvatar   varchar(1024)                          null comment 'user avatar',
    userProfile  varchar(512)                           null comment 'user profile',
    userRole     varchar(256) default 'user'            not null comment 'user role：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment 'edit time',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete     tinyint      default 0                 not null comment 'is delete 0-no, 1-yes',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment 'user' collate = utf8mb4_unicode_ci;

create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment 'app name',
    cover        varchar(512)                       null comment 'app cover',
    initPrompt   text                               null comment 'app initialization prompt',
    codeGenType  varchar(64)                        null comment 'code generation type (enum)',
    deployKey    varchar(64)                        null comment 'deployment key',
    deployedTime datetime                           null comment 'deployment time',
    priority     int      default 0                 not null comment 'priority',
    userId       bigint                             not null comment 'user id who created this app',
    editTime     datetime default CURRENT_TIMESTAMP not null comment 'edit time',
    createTime   datetime default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete     tinyint  default 0                 not null comment 'is delete 0-no, 1-yes',
    UNIQUE KEY uk_deployKey (deployKey), -- ensure deployment key is unique
    INDEX idx_appName (appName),         -- improve query performance based on app name
    INDEX idx_userId (userId)            -- improve query performance based on user id
) comment 'app' collate = utf8mb4_unicode_ci;

create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment 'message',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment 'app id',
    userId      bigint                             not null comment 'creator user id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment 'created time',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    isDelete    tinyint  default 0                 not null comment 'deleted or not',
    INDEX idx_appId (appId),                       -- improve query performance by app
    INDEX idx_createTime (createTime),             -- improve query performance by time
    INDEX idx_appId_createTime (appId, createTime) -- core index for cursor pagination
) comment 'chat history' collate = utf8mb4_unicode_ci;

