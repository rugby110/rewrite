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
package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.asClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URL

class OracleJdkParserTest {
    @JvmField @Rule val temp = TemporaryFolder()

    @Test
    fun parserIsAbleToIdentifyTypesFromExternalDependencies() {
        val testngDownloaded = URL("http://repo1.maven.org/maven2/org/testng/testng/6.9.9/testng-6.9.9.jar").openStream().readBytes()
        val testng = temp.newFile("testng-6.9.9.jar")
        testng.outputStream().use { it.write(testngDownloaded) }

        val a = OracleJdkParser(listOf(testng.toPath())).parse("""
            |package a;
            |import org.testng.annotations.*;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """)

        assertEquals("org.testng.annotations.Test", a.classes[0].methods()[0].annotations[0].type.asClass()?.fullyQualifiedName)
    }
}