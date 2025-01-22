---
title: 用户列表 (管理员)
---

请求地址: `/api/v1/user/list`

请求方式: `POST`

!!! note

    该接口用于获取仪表盘列表，需要传递分页数据 `page` 和 `size` 作为参数，分页数据默认为 `1`，`size` 默认为 `12`。详细参考 [分页实体](../common/page.md)

## Response

=== "示例"

    ```json
    {
      "id": 1,
      "name": null,
      "code": null,
      "active": true,
      "createTime": "2023-07-04 21:47:24",
      "updateTime": "2024-11-03 19:38:47",
      "username": "admin",
      "chatConfigure": "{\"type\":\"ChatGPT\",\"host\":null,\"token\":null,\"timeout\":0,\"contentCount\":5}",
      "system": false,
      "editorConfigure": {},
      "avatarConfigure": {
        "path": ""
      },
      "roles": []
    }
    ```

=== "参数"

    | 参数名 | 类型 | 描述 | 依赖      |
    | --- |----| --- |---------|
    | `id` | 数值 | 用户ID | `ADMIN` |
    |`name` | 字符串 | 用户名 | |
    |`code` | 字符串 | 用户编码 | |
    |`active` | 布尔值 | 是否激活 | `ADMIN` |
    |`createTime` | 字符串 | 创建时间 | |
    |`updateTime` | 字符串 | 更新时间 | |
    |`username` | 字符串 | 用户名 | |
    |`chatConfigure` | 字符串 | 聊天配置 | |
    |`system` | 布尔值 | 是否为系统用户 | `ADMIN` |
    |`editorConfigure` | 字符串 | 编辑器配置 | |
    |`avatarConfigure` | 字符串 | 头像配置 | |
    |`roles` | 数组 | 用户角色 | |
