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

object TreeBuilder {

    fun buildName(cache: TypeCache, fullyQualifiedName: String, fmt: Formatting = Formatting.Reified.Empty): NameTree {
        val parts = fullyQualifiedName.split('.')

        val expr = parts.foldIndexed(Tr.Empty(Formatting.None) as Expression to "") { i, acc, part ->
            val (target, subpackage) = acc
            if (target is Tr.Empty) {
                Tr.Ident(part, Type.Package.build(cache, part), Formatting.Reified.Empty) to part
            } else {
                val fullName = "$subpackage.$part"
                val partFmt = if (i == parts.size - 1) {
                    Formatting.Reified.Empty
                } else {
                    Formatting.Reified("", "\\s*[^\\s]+(\\s*)".toRegex().matchEntire(part)!!.groupValues[1])
                }

                val identFmt = Formatting.Reified("^\\s*".toRegex().find(part)!!.groupValues[0])

                if (part[0].isUpperCase() || i == parts.size - 1) {
                    Tr.FieldAccess(target, Tr.Ident(part.trim(), null, identFmt), Type.Class.build(cache, fullName), partFmt) to fullName
                } else {
                    Tr.FieldAccess(target, Tr.Ident(part.trim(), null, identFmt), Type.Package.build(cache, fullName), partFmt) to fullName
                }
            }
        }

        val outerExpr = expr.first as NameTree
        outerExpr.formatting = fmt

        return outerExpr
    }

    fun buildField(cache: TypeCache, modifiers: List<Tr.VariableDecls.Modifier>, clazz: String, name: String, init: Expression?) {
        val clazzTypeExpr = buildName(cache, clazz)
        Tr.VariableDecls(emptyList(), modifiers, clazzTypeExpr as TypeTree, null, emptyList(), listOf(Tr.VariableDecls.NamedVar(
                Tr.Ident(name, null, Formatting.Reified.Empty),
                emptyList(),
                init,
                clazzTypeExpr.type,
                Formatting.Reified(" ")
        )), Formatting.Infer)
    }
}