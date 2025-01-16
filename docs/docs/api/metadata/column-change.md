---
title: 修改数据列
---

请求地址: `/api/v1/metadata/{code}/{database}/{table}/change-column`

请求方式: `PUT`

## Request

=== "Path 示例"

    ```
    /api/v1/metadata/e98ef92476a94ce5ae28115d2eb7ff40/dc1/datacap_menu/change-column
    ```

=== "Body 示例"

    ```json
    {
        "columns": [
            {
                "name": "source_id",
                "type": "TINYINT",
                "length": 12332,
                "comment": "",
                "defaultValue": null,
                "primaryKey": false,
                "autoIncrement": false,
                "isNullable": "YES"
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
    |`columns`|Array|数据列信息|
    |`columns[].name`|String|数据列名称|
    |`columns[].type`|String|数据列类型|
    |`columns[].length`|Integer|数据列长度|
    |`columns[].comment`|String|数据列说明|
    |`columns[].defaultValue`|String|数据列默认值|
    |`columns[].primaryKey`|Boolean|数据列是否主键|
    |`columns[].autoIncrement`|Boolean|数据列是否自增|
    |`columns[].isNullable`|String|数据列是否可为空|

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