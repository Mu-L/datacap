---
title: 获取工作流
---

请求地址: `/api/v1/workflow/info/{code}`

请求方式: `GET`

## Response

=== "示例"

    ```json
    {
      "name": "sss",
      "code": "c1ff398243994338a79730f8a1d4f55f",
      "active": true,
      "createTime": "2024-12-19 22:19:30",
      "updateTime": "2024-12-19 22:19:31",
      "state": "FAILURE",
      "message": "/bin/sh: null/bin/start-seatunnel-spark-connector-v2.sh: No such file or directory",
      "work": "...",
      "elapsed": 492,
      "executor": "SeatunnelExecutor",
      "configure": { ... },
      "user": { ... }
    }
    ```

=== "参数"

    | 参数名 | 类型 | 描述 |
    | --- |----| --- |
    | `code` | 字符串 | 工作流编码 |
    | `name` | 字符串 | 名称 |
    | `executor` | 字符串 | 执行器 |
    | `active` | 布尔值 | 是否启用 |
    |`createTime`|字符串|创建时间|
    |`updateTime`|字符串|更新时间|
    |`state`|字符串|状态|
    |`message`|字符串|消息|
    |`work`|字符串|工作目录|
    |`elapsed`|整数|耗时|
    |`executor`|字符串|执行器|
    |`configure`|对象|配置|
    |`user`|对象|用户|
