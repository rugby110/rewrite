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

import java.io.Serializable
import java.util.*

sealed class Type(): Serializable {
    abstract class TypeWithOwner: Type() {
        abstract val owner: Type?

        // FIXME is this useful anymore?
        fun ownedByType(clazz: String): Boolean =
            if (this is Type.Class && fullyQualifiedName == clazz)
                true
            else if(owner is TypeWithOwner) 
                (owner as TypeWithOwner).ownedByType(clazz) 
            else false
    }
    
    data class Package private constructor(val fullName: String, override val owner: Type?): TypeWithOwner() {
        companion object {
            fun build(cache: TypeCache, fullName: String): Package? =
                if(fullName.isEmpty()) null
                else cache.packagePool.getOrPut(fullName) {
                    val subpackage = fullName.substringBeforeLast('.')
                    Package(fullName, if(subpackage != fullName) build(cache, subpackage) else null)
                }
        }
    }
    
    data class Class private constructor(val fullyQualifiedName: String,
                                         override val owner: Type?,
                                         var members: List<Var>,
                                         var supertype: Class?): TypeWithOwner() {

        override fun toString(): String = fullyQualifiedName

        fun isCyclicRef() = this == Cyclic

        fun className() =
                fullyQualifiedName.split('.').dropWhile { it[0].isLowerCase() }.joinToString(".")

        fun packageOwner() =
                fullyQualifiedName.split('.').dropLastWhile { it[0].isUpperCase() }.joinToString(".")

        companion object {
            val Cyclic = Class("CYCLIC_TYPE_REF", null, emptyList(), null)

            fun build(cache: TypeCache, fullyQualifiedName: String, members: List<Var> = emptyList(), supertype: Class? = null): Type.Class {
                val clazz = cache.classPool.getOrPut(fullyQualifiedName) {
                    val subName = fullyQualifiedName.substringBeforeLast(".")
                    Class(fullyQualifiedName,
                            if (!subName.contains('.'))
                                null
                            else if (subName.substringAfterLast('.').first().isUpperCase()) {
                                Class.build(cache, subName, emptyList(), null)
                            } else
                                Package.build(cache, subName),
                            members,
                            supertype)
                }

                if(members.isNotEmpty())
                    clazz.members = members
                if(supertype != null)
                    clazz.supertype = supertype

                return clazz
            }
        }
    }
    
    data class Method(val genericSignature: Signature,
                      val resolvedSignature: Signature,
                      val paramNames: List<String>?,
                      val flags: List<Flag>): Type() {

        fun hasFlags(vararg test: Flag) = test.all { flags.contains(it) }

        data class Signature(val returnType: Type?, val paramTypes: List<Type>)
    }
   
    data class GenericTypeVariable(val fullyQualifiedName: String, val bound: Class?): Type()
    
    data class Array(val elemType: Type): Type()
    
    data class Primitive(val typeTag: TypeTag): Type()
    
    data class Var(val name: String, val type: Type?, val flags: List<Flag>): Type() {

        fun hasFlags(vararg test: Flag) = test.all { flags.contains(it) }
    }
}

fun Type?.asClass(): Type.Class? = when(this) {
    is Type.Class -> this
    else -> null
}

fun Type?.asPackage(): Type.Package? = when(this) {
    is Type.Package -> this
    else -> null
}

fun Type?.asArray(): Type.Array? = when(this) {
    is Type.Array -> this
    else -> null
}

fun Type?.asGeneric(): Type.GenericTypeVariable? = when(this) {
    is Type.GenericTypeVariable -> this
    else -> null
}

fun Type?.asMethod(): Type.Method? = when(this) {
    is Type.Method -> this
    else -> null
}

fun Type?.hasElementType(fullyQualifiedName: String): Boolean = when(this) {
    is Type.Array -> this.elemType.hasElementType(fullyQualifiedName)
    is Type.Class -> this.fullyQualifiedName == fullyQualifiedName
    is Type.GenericTypeVariable -> this.fullyQualifiedName == fullyQualifiedName
    else -> false
}