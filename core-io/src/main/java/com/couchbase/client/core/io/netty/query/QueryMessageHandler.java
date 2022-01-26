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

package com.couchbase.client.core.io.netty.query;

import com.couchbase.client.core.endpoint.BaseEndpoint;
import com.couchbase.client.core.endpoint.EndpointContext;
import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.ErrorCodeAndMessage;
import com.couchbase.client.core.error.context.QueryErrorContext;
import com.couchbase.client.core.io.netty.chunk.ChunkedMessageHandler;
import com.couchbase.client.core.msg.query.QueryRequest;
import com.couchbase.client.core.msg.query.QueryResponse;
import com.couchbase.client.core.msg.query.QueryChunkHeader;
import com.couchbase.client.core.msg.query.QueryChunkRow;
import com.couchbase.client.core.msg.query.QueryChunkTrailer;
import com.couchbase.client.core.retry.RetryReason;

import java.util.List;
import java.util.Optional;

public class QueryMessageHandler
  extends ChunkedMessageHandler<QueryChunkHeader, QueryChunkRow, QueryChunkTrailer, QueryResponse, QueryRequest> {

  public QueryMessageHandler(BaseEndpoint endpoint, EndpointContext endpointContext) {
    super(endpoint, endpointContext, new QueryChunkResponseParser());
  }

  @Override
  protected Optional<RetryReason> qualifiesForRetry(final CouchbaseException exception) {
    if (exception.context() instanceof QueryErrorContext) {
      QueryErrorContext errorContext = (QueryErrorContext) exception.context();
      List<ErrorCodeAndMessage> errors = errorContext.errors();
      if (!errors.isEmpty()) {
        return mapError(errors.get(0));
      }
    }
    return Optional.empty();
  }

  /**
   * Check if the error code is eligible for transparent retry.
   * <p>
   * Note that while not explicitly necessary, the code calls out for prepared statement errors
   * 4040, 4050 and 4070 to NOT be retried at this level. They need to be handled at the higher
   * level since we need to clear the caches and re-prepare (which is a new op technically).
   *
   * @param error the error code to check.
   * @return has some value if eligible for retry.
   */
  private static Optional<RetryReason> mapError(final ErrorCodeAndMessage error) {
    int code = error.code();

    if (code == 4040 || code == 4050 || code == 4070) {
      // prepared errors not retried on purpose here.
      return Optional.empty();
    }

    if (code == 5000 && error.message().contains("queryport.indexNotFound")) {
      return Optional.of(RetryReason.QUERY_INDEX_NOT_FOUND);
    }

    if (code == 17013) {
      return Optional.of(RetryReason.QUERY_TRANSACTION_BUFFER_FULL);
    }

    if (error.retry()) {
      return Optional.of(RetryReason.QUERY_ERROR_RETRYABLE);
    }

    return Optional.empty();
  }

}
