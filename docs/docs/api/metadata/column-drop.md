---
title: 删除数据列
---

请求地址: `/api/v1/metadata/{code}/{database}/{table}/drop-column`

请求方式: `DELETE`

## Request

=== "Path 示例"

    ```
    /api/v1/metadata/e98ef92476a94ce5ae28115d2eb7ff40/dc1/datacap_menu/drop-column
    ```

=== "Body 示例"

    ```json
    {
        "preview": false,
        "columns": [
            {
                "name": "scheduled_history_id"
            }
        ]
    }
    ```

=== "Path 参数"

    |参数|类型|描述|
    |---|---|---|
    |`code`|String|数据源编码|
    |`database`|String|数据库名称|
    |`table`|String|数据表名称|

=== "Body 参数"

    |参数|类型|描述|
    |---|---|---|
    |`preview`|Boolean|是否预览要执行的 SQL|
    |`columns`|Array|列名列表|
    |`columns[n].name`|String|列名|

## Response

=== "示例"

    ```json
    {
        "result": 0
    }
    ```

=== "参数"

    |参数|类型|描述|
    |---|---|---|
    |`result`|Integer|操作结果|