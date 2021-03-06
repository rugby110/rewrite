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

import java.util.*

data class TypeCache private constructor(val key: String) {
    val packagePool = HashMap<String, Type.Package>()
    val classPool = HashMap<String, Type.Class>()

    companion object {
        private val caches = WeakHashMap<String, TypeCache>()
        val random = Random()

        fun of(key: String): TypeCache = caches.getOrPut(key) { TypeCache(key) }

        fun new(): TypeCache {
            val buffer = ByteArray(5)
            random.nextBytes(buffer)
            val uid = Base64.getEncoder().encodeToString(buffer)
            val cache = TypeCache(uid)
            caches.put(uid, cache)
            return cache
        }
    }

    fun reset() {
        packagePool.clear()
        classPool.clear()
    }
}