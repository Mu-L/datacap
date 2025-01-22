---
title: 流水线
status: 1.14.0
---

在 DataCap 系统中，流水线功能用户可以随意配置，根据低层执行器的版本可以任意调整，系统会根据配置自动识别并编译最终的配置并发送到执行器中。

我们可提供的配置有:

|       字段        |     类型      |                           描述                           |
|:---------------:|:-----------:|:------------------------------------------------------:|
|     `field`     |  `String`   |                          字段名                           |
|    `origin`     |  `String`   | 默认等于字段值，自定义列名使用, 如果是 `host|port` 格式，系统将会将字段通过 `:` 进行拼接  |
|   `required`    |  `Boolean`  |                 当值为 `true` 时，表示该字段为必填项                 |
|   `override`    |  `Boolean`  | 如果该标志为 `true`，则表示通过用户配置提取该字段，默认数据将被丢弃 |
|     `input`     |  `Boolean`  |                        是否为输入参数                         |
|     `width`     |  `Integer`  |                     组件宽度, 默认 `300`                     |
|     `type`      | `FieldType` |                    字段类型, 默认 `INPUT`                    |
|    `tooltip`    |  `String`   |                          提示信息                          |
|  `description`  |  `String`   |                          描述信息                          |
|     `value`     |  `Object`   |                       当前配置输入的结果                        |
|    `hidden`     |  `Boolean`  |            如果该配置项为 `true`，则前端不会显示，启用后才会显示。             |
| `defaultValues` |   `Array`   |                如果类型为 SELECT ，则需要传入默认数据                 |

!!! danger

    我们不建议用户自行修改以上配置，如有出现配置异常将会导致插件无法使用该功能
