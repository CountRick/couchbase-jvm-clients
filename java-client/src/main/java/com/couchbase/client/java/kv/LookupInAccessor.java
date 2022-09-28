/*
 * Copyright (c) 2018 Couchbase, Inc.
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

package com.couchbase.client.java.kv;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.error.context.KeyValueErrorContext;
import com.couchbase.client.core.msg.ResponseStatus;
import com.couchbase.client.core.msg.kv.SubdocGetRequest;
import com.couchbase.client.java.codec.JsonSerializer;

import java.util.concurrent.CompletableFuture;

import static com.couchbase.client.core.error.DefaultErrorUtil.keyValueStatusToException;

public class LookupInAccessor {

  public static CompletableFuture<LookupInResult> lookupInAccessor(final Core core, final SubdocGetRequest request,
                                                                   final JsonSerializer serializer) {
    core.send(request);
    return request
      .response()
      .thenApply(response -> {
        if (response.status().success()) {
          return new LookupInResult(response.values(), response.cas(), serializer, null, response.isDeleted());
        } else if (response.status() == ResponseStatus.SUBDOC_FAILURE) {
          final KeyValueErrorContext ctx = KeyValueErrorContext.completedRequest(request, response);
          return new LookupInResult(response.values(), response.cas(), serializer, ctx, response.isDeleted());
        }
        throw keyValueStatusToException(request, response);
      }).whenComplete((t, e) -> {
              if (e == null || e instanceof DocumentNotFoundException) {
                request.context().logicallyComplete();
              } else {
                request.context().logicallyComplete(e);
              }
            });
  }
}
