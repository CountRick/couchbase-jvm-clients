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
import com.couchbase.client.core.error.InvalidArgumentException;
import com.couchbase.client.java.CommonOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.couchbase.client.core.util.CbStrings.emptyToNull;
import static com.couchbase.client.core.util.Validators.notNull;
import static com.couchbase.client.core.util.Validators.notNullOrEmpty;

/**
 * Allows customizing how a query primary index is created.
 */
public class CreatePrimaryQueryIndexOptions extends CommonOptions<CreatePrimaryQueryIndexOptions> {

  private final Map<String, Object> with = new HashMap<>();

  private Optional<String> indexName = Optional.empty();
  private boolean ignoreIfExists;
  private String scopeName;
  private String collectionName;

  private CreatePrimaryQueryIndexOptions() {
  }

  /**
   * Creates a new instance with default values.
   *
   * @return the instantiated default options.
   */
  public static CreatePrimaryQueryIndexOptions createPrimaryQueryIndexOptions() {
    return new CreatePrimaryQueryIndexOptions();
  }

  /**
   * The custom name of the primary index.
   *
   * @param indexName the custom name of the primary index.
   * @return this options builder for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions indexName(final String indexName) {
    this.indexName = Optional.ofNullable(emptyToNull(indexName));
    return this;
  }

  /**
   * Set to true if no exception should be thrown if the primary index already exists (false by default).
   *
   * @param ignoreIfExists true if no exception should be thrown if the index already exists.
   * @return this options builder for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions ignoreIfExists(final boolean ignoreIfExists) {
    this.ignoreIfExists = ignoreIfExists;
    return this;
  }

  /**
   * Configures the number of index replicas.
   *
   * @param numReplicas the number of replicas used for this index.
   * @return this options builder for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions numReplicas(final int numReplicas) {
    if (numReplicas < 0) {
      throw InvalidArgumentException.fromMessage("numReplicas must be >= 0");
    }
    return with("num_replica", numReplicas);
  }

  /**
   * If set to true will defer building of this index (false by default).
   * <p>
   * If you are creating multiple indexes on the same bucket, you may see improved performance by creating
   * them in deferred mode and then building them all at once. Please use
   * {@link QueryIndexManager#buildDeferredIndexes(String)} afterwards to build the deferred indexes.
   *
   * @param deferred if building this index should be deferred.
   * @return this options builder for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions deferred(final boolean deferred) {
    return with("defer_build", deferred);
  }

  /**
   * Allows passing in custom options into the N1QL WITH clause for index creation.
   * <p>
   * This method should only be used if no other option is available - use with caution!
   *
   * @param optionName the name of the WITH option.
   * @param optionValue the value of the WITH option.
   * @return this options builder for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions with(final String optionName, final Object optionValue) {
    this.with.put(notNullOrEmpty(optionName, "OptionName"), notNull(optionValue, "OptionValue"));
    return this;
  }

  /**
   * Sets the scope name for this query management operation.
   * <p>
   * Please note that if the scope name is set, the {@link #collectionName(String)} (String)} must also be set.
   *
   * @param scopeName the name of the scope.
   * @return this options class for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions scopeName(final String scopeName) {
    this.scopeName = notNullOrEmpty(scopeName, "ScopeName");
    return this;
  }

  /**
   * Sets the collection name for this query management operation.
   * <p>
   * Please note that if the collection name is set, the {@link #scopeName(String)} must also be set.
   *
   * @param collectionName the name of the collection.
   * @return this options class for chaining purposes.
   */
  public CreatePrimaryQueryIndexOptions collectionName(final String collectionName) {
    this.collectionName = notNullOrEmpty(collectionName, "CollectionName");
    return this;
  }

  @Stability.Internal
  public Built build() {
    if (collectionName != null && scopeName == null) {
      throw InvalidArgumentException.fromMessage("If a collectionName is provided, a scopeName must also be provided");
    }
    if (scopeName != null && collectionName == null) {
      throw InvalidArgumentException.fromMessage("If a scopeName is provided, a collectionName must also be provided");
    }
    return new Built();
  }

  public class Built extends BuiltCommonOptions {
    Built() { }
    public boolean ignoreIfExists() {
      return ignoreIfExists;
    }

    public Optional<String> indexName() {
      return indexName;
    }

    public Map<String, Object> with() {
      return with;
    }

    public Optional<String> scopeName() {
      return Optional.ofNullable(scopeName);
    }

    public Optional<String> collectionName() {
      return Optional.ofNullable(collectionName);
    }
  }
}
