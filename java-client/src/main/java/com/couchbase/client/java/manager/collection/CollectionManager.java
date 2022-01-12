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

package com.couchbase.client.java.manager.collection;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.CollectionExistsException;
import com.couchbase.client.core.error.CollectionNotFoundException;
import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.ScopeExistsException;
import com.couchbase.client.core.error.ScopeNotFoundException;
import com.couchbase.client.java.Bucket;

import java.util.List;

import static com.couchbase.client.java.AsyncUtils.block;
import static com.couchbase.client.java.manager.collection.CreateCollectionOptions.createCollectionOptions;
import static com.couchbase.client.java.manager.collection.CreateScopeOptions.createScopeOptions;
import static com.couchbase.client.java.manager.collection.DropCollectionOptions.dropCollectionOptions;
import static com.couchbase.client.java.manager.collection.DropScopeOptions.dropScopeOptions;
import static com.couchbase.client.java.manager.collection.GetAllScopesOptions.getAllScopesOptions;
import static com.couchbase.client.java.manager.collection.GetScopeOptions.getScopeOptions;

/**
 * The {@link CollectionManager} provides APIs to manage bucket collections and scopes.
 */
@Stability.Volatile
public class CollectionManager {

  /**
   * The underlying async collection manager.
   */
  private final AsyncCollectionManager asyncCollectionManager;

  /**
   * Creates a new {@link CollectionManager}.
   * <p>
   * This API is not intended to be called by the user directly, use {@link Bucket#collections()}
   * instead.
   *
   * @param async the underlying async collection manager.
   */
  @Stability.Internal
  public CollectionManager(final AsyncCollectionManager async) {
    this.asyncCollectionManager = async;
  }


  /**
   * Creates a collection if it does not already exist.
   * <p>
   * Note that a scope needs to be created first (via {@link #createScope(String)}) if it doesn't exist already.
   *
   * @param collectionSpec the collection spec that contains the properties of the collection.
   * @throws CollectionExistsException if the collection already exists
   * @throws ScopeNotFoundException if the specified scope does not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void createCollection(final CollectionSpec collectionSpec) {
    createCollection(collectionSpec, createCollectionOptions());
  }

  /**
   * Creates a collection if it does not already exist with custom options.
   * <p>
   * Note that a scope needs to be created first (via {@link #createScope(String)}) if it doesn't exist already.
   *
   * @param collectionSpec the collection spec that contains the properties of the collection.
   * @param options the custom options to apply.
   * @throws CollectionExistsException if the collection already exists
   * @throws ScopeNotFoundException if the specified scope does not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void createCollection(final CollectionSpec collectionSpec, final CreateCollectionOptions options) {
    block(asyncCollectionManager.createCollection(collectionSpec, options));
  }

  /**
   * Creates a scope if it does not already exist.
   *
   * @param scopeName the name of the scope to create.
   * @throws ScopeExistsException if the scope already exists.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void createScope(final String scopeName) {
    createScope(scopeName, createScopeOptions());
  }

  /**
   * Creates a scope if it does not already exist with custom options.
   *
   * @param scopeName the name of the scope to create.
   * @param options the custom options to apply.
   * @throws ScopeExistsException if the scope already exists.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void createScope(final String scopeName, final CreateScopeOptions options) {
    block(asyncCollectionManager.createScope(scopeName, options));
  }

  /**
   * Drops a collection if it exists.
   *
   * @param collectionSpec the collection spec that contains the properties of the collection.
   * @throws CollectionNotFoundException if the collection did not exist.
   * @throws ScopeNotFoundException if the specified scope does not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void dropCollection(final CollectionSpec collectionSpec) {
    dropCollection(collectionSpec, dropCollectionOptions());
  }

  /**
   * Drops a collection if it exists with custom options.
   *
   * @param collectionSpec the collection spec that contains the properties of the collection.
   * @param options the custom options to apply.
   * @throws CollectionNotFoundException if the collection did not exist.
   * @throws ScopeNotFoundException if the specified scope does not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void dropCollection(final CollectionSpec collectionSpec, final DropCollectionOptions options) {
    block(asyncCollectionManager.dropCollection(collectionSpec, options));
  }

  /**
   * Drops a scope if it exists.
   *
   * @param scopeName the name of the scope to drop.
   * @throws ScopeNotFoundException if the scope did not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void dropScope(final String scopeName) {
    dropScope(scopeName, dropScopeOptions());
  }

  /**
   * Drops a scope if it exists with custom options.
   *
   * @param scopeName the name of the scope to drop.
   * @param options the custom options to apply.
   * @throws ScopeNotFoundException if the scope did not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public void dropScope(final String scopeName, final DropScopeOptions options) {
    block(asyncCollectionManager.dropScope(scopeName, options));
  }

  /**
   * Returns the scope if it exists.
   *
   * @param scopeName the name of the scope.
   * @return containing information about the scope.
   * @throws ScopeNotFoundException if scope does not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   * @deprecated use {@link #getAllScopes()} instead.
   */
  @Deprecated
  public ScopeSpec getScope(final String scopeName) {
    return getScope(scopeName, getScopeOptions());
  }

  /**
   * Returns the scope if it exists with custom options.
   *
   * @param scopeName the name of the scope.
   * @param options the custom options to apply.
   * @return containing information about the scope.
   * @throws ScopeNotFoundException if scope does not exist.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   * @deprecated use {@link #getAllScopes(GetAllScopesOptions)} instead.
   */
  @Deprecated
  public ScopeSpec getScope(final String scopeName, final GetScopeOptions options) {
    return block(asyncCollectionManager.getScope(scopeName, options));
  }

  /**
   * Returns all scopes in this bucket.
   *
   * @return a (potentially empty) list of scopes in the bucket.
   * @throws CouchbaseException if any other generic unhandled/unexpected errors.
   */
  public List<ScopeSpec> getAllScopes() {
    return getAllScopes(getAllScopesOptions());
  }

  /**
   * Returns all scopes in this bucket with custom options.
   *
   * @param options the custom options to apply.
   * @return a (potentially empty) list of scopes in the bucket.
   * @throws CouchbaseException  if any other generic unhandled/unexpected errors.
   */
  public List<ScopeSpec> getAllScopes(final GetAllScopesOptions options) {
    return block(asyncCollectionManager.getAllScopes(options));
  }

}
