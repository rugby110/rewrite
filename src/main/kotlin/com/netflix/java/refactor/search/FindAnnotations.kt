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
package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.AnnotationMatcher
import com.netflix.java.refactor.ast.Tr
import com.netflix.java.refactor.ast.visitor.AstVisitor

class FindAnnotations(signature: String) : AstVisitor<List<Tr.Annotation>>(emptyList()) {
    private val matcher = AnnotationMatcher(signature)

    override fun visitAnnotation(annotation: Tr.Annotation): List<Tr.Annotation> {
        return if (matcher.matches(annotation)) listOf(annotation) else emptyList()
    }
}
