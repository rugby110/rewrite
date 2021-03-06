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
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import java.util.ArrayList
import com.netflix.java.refactor.refactor.RefactorVisitor
import com.netflix.java.refactor.search.FindType

class AddImport(val clazz: String,
                val staticMethod: String? = null,
                val onlyIfReferenced: Boolean = false): RefactorVisitor() {
    private var coveredByExistingImport = false
    private val packageComparator = PackageComparator()
    private val classType by lazy { Type.Class.build(cu.typeCache(), clazz) }

    private var hasReferences: Boolean = false

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): List<AstTransform<*>> {
        hasReferences = FindType(clazz).visit(cu).isNotEmpty()
        return super.visitCompilationUnit(cu)
    }

    override fun visitImport(import: Tr.Import): List<AstTransform<*>> {
        val importedType = import.qualid.simpleName

        if (addingStaticImport()) {
            if (import.matches(clazz) && import.static && (importedType == staticMethod || importedType == "*")) {
                coveredByExistingImport = true
            }
        }
        else {
            if (import.matches(clazz)) {
                coveredByExistingImport = true
            } else if (import.qualid.target.printTrimmed() == classType.packageOwner() && importedType == "*") {
                coveredByExistingImport = true
            }
        }

        return emptyList()
    }

    override fun visitEnd(): List<AstTransform<*>> {
        if(onlyIfReferenced && !hasReferences)
            return emptyList()

        if(classType.packageOwner().isEmpty())
            return emptyList()

        val lastPrior = lastPriorImport()
        val classImportField = TreeBuilder.buildName(cu.typeCache(), clazz, Formatting.Reified(" ")) as Tr.FieldAccess

        val importStatementToAdd = if(addingStaticImport()) {
            Tr.Import(Tr.FieldAccess(classImportField, Tr.Ident(staticMethod!!, null, Formatting.Reified.Empty), null, Formatting.Reified.Empty), true, Formatting.Infer)
        } else Tr.Import(classImportField, false, Formatting.Infer)

        return if(coveredByExistingImport) {
            emptyList()
        }
        else if(lastPrior == null) {
            listOf(AstTransform<Tr.CompilationUnit>(cursor()) {
                copy(imports = listOf(importStatementToAdd) + cu.imports)
            })
        }
        else {
            listOf(AstTransform<Tr.CompilationUnit>(cursor()) {
                copy(imports = cu.imports.takeWhile { it !== lastPrior } + listOf(lastPrior, importStatementToAdd) +
                        cu.imports.takeLastWhile { it !== lastPrior })
            })
        }
    }

    fun lastPriorImport(): Tr.Import? {
        return cu.imports.lastOrNull { import ->
            // static imports go after all non-static imports
            if(addingStaticImport() && !import.static)
                return@lastOrNull true

            // non-static imports should always go before static imports
            if(!addingStaticImport() && import.static)
                return@lastOrNull false

            val comp = packageComparator.compare(import.qualid.target.printTrimmed(),
                    if(addingStaticImport()) clazz else classType.packageOwner())
            if(comp == 0) {
                if(import.qualid.simpleName < if(addingStaticImport()) staticMethod!! else classType.className()) {
                    true
                }
                else false
            }
            else if(comp < 0) true
            else false
        }
    }

    fun addingStaticImport() = staticMethod is String
}