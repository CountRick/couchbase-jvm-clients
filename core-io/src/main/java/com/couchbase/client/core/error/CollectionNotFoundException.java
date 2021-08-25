/*
 * Copyright 2019 Couchbase, Inc.
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

package com.couchbase.client.core.error;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.context.ErrorContext;
import com.couchbase.client.core.util.CbStrings;

import static com.couchbase.client.core.logging.RedactableArgument.redactMeta;
import static com.couchbase.client.core.util.CbStrings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@Stability.Volatile
public class CollectionNotFoundException extends CouchbaseException {

  private final String collectionName;

  public CollectionNotFoundException(String collectionName) {
    this(collectionName, null);
  }

  public CollectionNotFoundException(String collectionName, ErrorContext errorContext) {
    super("Collection" + formatCollection(collectionName) + " not found.", errorContext);
    this.collectionName = requireNonNull(collectionName);
  }

  private static String formatCollection(String collectionName) {
    return isNullOrEmpty(collectionName) ? "" : " [" + redactMeta(collectionName) + "]";
  }

  public static CollectionNotFoundException forCollection(String collectionName) {
    return new CollectionNotFoundException(collectionName);
  }

  public String collectionName() {
    return collectionName;
  }
}
