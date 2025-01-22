package io.edurt.datacap.executor.configure

import io.edurt.datacap.executor.common.RunState

data class ExecutorResponse(var timeout: Boolean = false,
                            var successful: Boolean = false,
                            var state: RunState? = null,
                            var message: String? = null,
                            var count: Int = 0) {
    constructor(timeout: Boolean = false,
                successful: Boolean = false,
                state: RunState? = null,
                message: String? = null) : this(timeout, successful, state, message, 0)
}
