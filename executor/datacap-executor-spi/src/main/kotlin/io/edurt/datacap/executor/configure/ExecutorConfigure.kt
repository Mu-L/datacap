package io.edurt.datacap.executor.configure

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.edurt.datacap.executor.common.RunProtocol
import io.edurt.datacap.spi.PluginService
import io.edurt.datacap.spi.model.Configure
import java.util.*

@SuppressFBWarnings(value = ["EI_EXPOSE_REP", "EI_EXPOSE_REP2"])
data class ExecutorConfigure(
    var type: String?,
    var configure: Properties?,
    var supportOptions: Set<String> = mutableSetOf(),
    var protocol: String?,
    var plugin: PluginService? = null,
    var query: String? = null,
    var database: String? = null,
    var table: String? = null,
    var originConfigure: Configure? = null,
    var originColumns: Set<OriginColumn> = mutableSetOf()
)
{

    constructor(type: String?) : this(type, null, mutableSetOf())

    constructor(
        type: String?,
        configure: Properties?,
        supportOptions: Set<String> = mutableSetOf()
    ) : this(
        type,
        configure,
        supportOptions,
        RunProtocol.NONE.name,
        null,
        null,
        null,
        null,
        null
    )

    constructor(
        type: String,
        configure: Properties,
        supportOptions: Set<String> = mutableSetOf(),
        protocol: String
    ) : this(
        type,
        configure,
        supportOptions,
        protocol,
        null,
        null,
        null,
        null,
        null
    )
}
