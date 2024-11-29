package io.edurt.datacap.test.qiniu

import io.edurt.datacap.fs.qiniu.IOUtils
import io.edurt.datacap.test.BaseIOUtilsTest

class IOUtilsTest : BaseIOUtilsTest(
    pluginPrefix = "qiniu",
    ioUtils = IOUtils::class.java
)
