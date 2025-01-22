---
title: 仪表盘列表
---

请求地址：`/api/v1/dashboard/list`

请求方式：`GET`

!!! note

    该接口用于获取仪表盘列表，需要传递分页数据 `page` 和 `size` 作为参数，分页数据默认为 `1`，`size` 默认为 `12`。详细参考 [分页实体](../common/page.md)

## Response

=== "示例"

    ```json
    [
      {
        "id": 1,
        "name": "第一个仪表盘",
        "code": "8ee2171b5d014779a45901fb9c2428c9",
        "active": true,
        "createTime": "2024-11-17 14:33:29",
        "updateTime": "2024-11-18 16:41:53",
        "configure": "",
        "description": "",
        "avatar": {
          "path": "" 
        },
        "user": {},
        "reports": []
      }
    ]
    ```

=== "参数"

    |参数|类型|描述|权限|
    |---|---|---|---|
    |`id`|Number|分页数据|`Admin`|
    |`name`|String|仪表盘名称|-|
    |`code`|String|仪表盘编码|-|
    |`active`|Boolean|是否激活|-|
    |`createTime`|String|创建时间|-|
    |`updateTime`|String|更新时间|-|
    |`configure`|String|仪表盘配置|-|
    |`description`|String|仪表盘描述|-|
    |`avatar`|Avatar|仪表盘头像|-|
    |`user`|User|创建人|`Admin`|
    |`reports`|Report[]|报表列表|-|
