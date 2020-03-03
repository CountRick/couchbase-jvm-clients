/*
 * Copyright (c) 2019 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.core.error;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.context.ErrorContext;

/**
 * Indicates an operation failed because there has been an issue with query prepared statements.
 *
 * @since 3.0
 */
public class PreparedStatementFailureException extends CouchbaseException {

  private final boolean retryable;

  public PreparedStatementFailureException(final ErrorContext ctx, final boolean retryable) {
    super("Prepared statement failure", ctx);
    this.retryable = retryable;
  }

  @Stability.Internal
  public boolean retryable() {
    return retryable;
  }

}
