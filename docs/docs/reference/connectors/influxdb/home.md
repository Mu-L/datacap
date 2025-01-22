---
title: InfluxDB
---

<img src="/assets/plugin/influxdb.svg" class="connector-content-logo" />

InfluxDB 是一个由 InfluxData 开发的开源时序型数据库。它由 Go 写成，着力于高性能地查询与存储时序型数据。InfluxDB 被广泛应用于存储系统的监控数据。

#### 描述

---

DataCap 整合 InfluxDB 模块，用于实现对 InfluxDB 数据源的数据操作。

在该模块中我们使用的是 `influxdb-jdbc` 依赖，版本为 `0.2.6`。

使用到的驱动器为 `net.suteren.jdbc.influxdb.InfluxDbDriver`。

驱动源码可参考：https://github.com/konikvranik/jdbc-influxdb/

#### 操作

---

|    操作    | 是否支持 |
|:--------:|:----:|
| `SELECT` |  ✅   |

> 所有驱动器支持的操作均支持。

#### 环境

---

!!! note

    如果需要使用该数据源，需要将 DataCap 服务升级到 >= `2024.03.7`

支持时间: `2024-06-26`

#### 配置

---


!!! note

    如果您的服务版本需要其他特殊配置，请参考修改配置文件并重启 DataCap 服务。

=== "基本配置"

    | 属性 | 是否必要 | 默认值 | 备注 |
    |:---:|:---:|:---:|:---:|
    | `名称` | ✅ | - |-|
    | `主机地址` | ✅ | `127.0.0.1` | - |
    | `端口` | ✅ | `8086` | - |

=== "授权配置"

    | 属性 | 是否必要 | 默认值 |
    |:---:|:---:|:---:|
    | `用户名` | ❌ | - |
    | `密码` | ❌ | - |

=== "自定义"

    > 可以添加所有 InfluxDB 驱动支持的配置，方式为 key = value

    默认:
    
    | 属性 | 默认值 |
    |:---:|:---:|
    | `database` | `default` |
    