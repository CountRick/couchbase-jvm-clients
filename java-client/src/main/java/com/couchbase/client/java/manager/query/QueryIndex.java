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

package com.couchbase.client.java.manager.query;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;

import java.util.Objects;
import java.util.Optional;

import static com.couchbase.client.core.logging.RedactableArgument.redactMeta;
import static com.couchbase.client.core.util.CbObjects.defaultIfNull;
import static com.couchbase.client.core.util.CbStrings.emptyToNull;
import static java.util.Objects.requireNonNull;

/**
 * Contains the properties of a Query Index.
 */
public class QueryIndex {

  private final String name;
  private final boolean primary;
  private final String state;
  private final String keyspace;
  private final String namespace;
  private final JsonArray indexKey;
  private final String type;
  private final Optional<String> condition;
  private final Optional<String> partition;
  private final JsonObject raw;
  private final Optional<String> scopeName;
  private final Optional<String> bucketName;

  @Stability.Internal
  QueryIndex(final JsonObject raw) {
    this.raw = requireNonNull(raw);

    this.name = raw.getString("name");
    this.state = raw.getString("state");
    this.keyspace = raw.getString("keyspace_id");
    this.namespace = raw.getString("namespace_id");
    this.indexKey = raw.getArray("index_key");
    this.condition = Optional.ofNullable(emptyToNull(raw.getString("condition")));
    this.primary = Boolean.TRUE.equals(raw.getBoolean("is_primary"));
    this.type = defaultIfNull(raw.getString("using"), "gsi");
    this.partition = Optional.ofNullable(emptyToNull(raw.getString("partition")));
    this.scopeName = Optional.ofNullable(emptyToNull(raw.getString("scope_id")));
    this.bucketName = Optional.ofNullable(emptyToNull(raw.getString("bucket_id")));
  }

  /**
   * True if this index is a primary index.
   *
   * @return true if this index is a primary index.
   */
  public boolean primary() {
    return this.primary;
  }

  /**
   * Returns the name of this index.
   *
   * @return the name of the index.
   */
  public String name() {
    return this.name;
  }

  /**
   * Returns the index type (most likely "gsi").
   *
   * @return the type of the index.
   */
  public String type() {
    return type;
  }

  /**
   * Returns the state in which the index is in (i.e. "online").
   *
   * @return the state of the index.
   */
  public String state() {
    return state;
  }

  /**
   * Returns the keyspace of this index.
   * <p>
   * If the index is at the bucket-level, this will return the bucket name. If the index is at the collection-level,
   * the keyspace is the name of the collection.
   *
   * @return the keyspace of this index.
   */
  public String keyspace() {
    return keyspace;
  }

  /**
   * @return the namespace for the index. A namespace is a resource pool that contains multiple keyspaces.
   * @see #keyspace()
   */

  /**
   * Returns the namespace of this index.
   * <p>
   * The namespace should not be confused with the keyspace - the namespace usually is "default".
   *
   * @return the namespace of this index.
   */
  public String namespace() {
    return namespace;
  }

  /**
   * Returns an {@link JsonArray array} of Strings that represent the index key(s).
   * <p>
   * The array is empty in the case of a PRIMARY INDEX.
   * <p>
   * Note that the query service can present the key in a slightly different manner from when you declared the index:
   * for instance, it will show the indexed fields in an escaped format (surrounded by backticks).
   *
   * @return an array of Strings that represent the index key(s), or an empty array in the case of a PRIMARY index.
   */
  public JsonArray indexKey() {
    return this.indexKey;
  }

  /**
   * Returns the {@link String} representation of the index's condition (the WHERE clause of the index), or an empty
   * Optional if no condition was set.
   * <p>
   * Note that the query service can present the condition in a slightly different manner from when you declared the index:
   * for instance it will wrap expressions with parentheses and show the fields in an escaped format (surrounded by
   * backticks).
   *
   * @return the condition/WHERE clause of the index or empty string if none.
   */
  public Optional<String> condition() {
    return this.condition;
  }

  /**
   * If present, returns the configured partition for the index.
   *
   * @return the partition if set, empty if none.
   */
  public Optional<String> partition() {
    return partition;
  }

  /**
   * If present, returns the name of the scope this index is stored in.
   *
   * @return the name of the scope, if present.
   */
  public Optional<String> scopeName() {
    return scopeName;
  }

  /**
   * If present, returns the name of the bucket this index is stored in.
   *
   * @return the name of the bucket, if present.
   */
  public String bucketName() {
    return bucketName.orElse(keyspace);
  }

  /**
   * If present, returns the name of the collection this index is stored in.
   *
   * @return the name of the collection, if present.
   */
  public Optional<String> collectionName() {
    return bucketName.isPresent() && scopeName.isPresent() ? Optional.of(keyspace) : Optional.empty();
  }

  /**
   * Returns the JSON as it arrived from the server.
   *
   * @return the raw JSON representation of the index information, as returned by the query service.
   */
  public JsonObject raw() {
    return raw;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueryIndex that = (QueryIndex) o;
    return raw.equals(that.raw);
  }

  @Override
  public int hashCode() {
    return Objects.hash(raw);
  }

  @Override
  public String toString() {
    return redactMeta(raw).toString();
  }
}
