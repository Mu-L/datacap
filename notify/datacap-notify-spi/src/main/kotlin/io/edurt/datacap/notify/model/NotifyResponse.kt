package io.edurt.datacap.notify.model

class NotifyResponse
{
    var successful: Boolean = false
    var message: String? = null

    override fun toString(): String
    {
        return "NotifyResponse(successful=$successful, message=$message)"
    }
}
