/*
 * Copyright 2021 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.kotlin

import com.couchbase.client.core.cnc.RequestSpan
import com.couchbase.client.core.endpoint.http.CoreCommonOptions
import com.couchbase.client.core.retry.RetryStrategy
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Options that apply most requests.
 */
public class CommonOptions(
    public val timeout: Duration? = null,
    public val parentSpan: RequestSpan? = null,
    public val retryStrategy: RetryStrategy? = null,
    public val clientContext: Map<String, Any?>? = null,
) {
    public companion object {
        public val Default: CommonOptions = CommonOptions()

        private val CoreDefault: CoreCommonOptions = CoreCommonOptions.of(null, null, null)
    }

    internal fun toCore(): CoreCommonOptions =
        if (this === Default) CoreDefault else CoreCommonOptions.of(timeout?.toJavaDuration(),
            retryStrategy,
            parentSpan)
}
