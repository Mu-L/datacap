package io.edurt.datacap.convert.none

import com.google.inject.Guice.createInjector
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import io.edurt.datacap.convert.Convert
import io.edurt.datacap.convert.ConvertManager
import org.junit.Assert.assertEquals
import org.junit.Test

class NoneModuleTest
{
    private val injector: Injector = createInjector(ConvertManager())

    @Test
    fun test()
    {
        injector.getInstance(Key.get(object : TypeLiteral<Set<Convert>>()
        {}))
            .stream()
            .findFirst()
            .ifPresent {
                assertEquals("None", it.name())
            }
    }
}
