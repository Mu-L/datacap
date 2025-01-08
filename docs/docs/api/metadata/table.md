---
title: 数据表列表
---

请求地址: `/api/v1/metadata/{code}/tables/{db}`

请求方式: `POST`

## Path

=== "示例"

    ```
    /api/v1/metadata/e98ef92476a94ce5ae28115d2eb7ff40/tables/infosphere
    ```

=== "参数"

    |参数|类型|描述|
    |---|---|---|
    |`code`|String|数据源编码|
    |`db`|String|数据库名称|

## Response

=== "示例"

    ```json
    {
      "object_name": "authx",
      "object_type": "VIEW",
      "object_comment": ""
    }
    ```

=== "参数"

    |参数|类型|描述|
    |---|---|---|
    |`object_name`|String|对象名称|
    |`object_type`|String|对象类型|
    |`object_comment`|String|对象描述|