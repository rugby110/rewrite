/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class AnnotationTest(p: Parser): Parser by p {
    
    @Test
    fun annotation() {
        val a = parse("""
            |@SuppressWarnings("ALL")
            |public class A {}
        """)
        
        val ann = a.classes[0].annotations[0]
        
        assertEquals("java.lang.SuppressWarnings", ann.type.asClass()?.fullyQualifiedName)
        assertEquals("ALL", ann.args!!.args.filterIsInstance<Tr.Literal>().firstOrNull()?.value)
    }
    
    @Test
    fun formatImplicitDefaultArgument() {
        val a = parse("""
            |@SuppressWarnings("ALL")
            |public class A {}
        """)
        
        val ann = a.classes[0].annotations[0]
        
        assertEquals("@SuppressWarnings(\"ALL\")", ann.printTrimmed())
    }
}