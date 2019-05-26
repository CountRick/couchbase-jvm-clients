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

package com.couchbase.client.java;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.client.core.msg.kv.AppendRequest;
import com.couchbase.client.core.msg.kv.DecrementRequest;
import com.couchbase.client.core.msg.kv.IncrementRequest;
import com.couchbase.client.core.msg.kv.PrependRequest;
import com.couchbase.client.core.retry.RetryStrategy;
import com.couchbase.client.java.kv.AppendAccessor;
import com.couchbase.client.java.kv.AppendOptions;
import com.couchbase.client.java.kv.CounterAccessor;
import com.couchbase.client.java.kv.CounterResult;
import com.couchbase.client.java.kv.DecrementOptions;
import com.couchbase.client.java.kv.IncrementOptions;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.PrependAccessor;
import com.couchbase.client.java.kv.PrependOptions;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.couchbase.client.core.util.Validators.notNull;
import static com.couchbase.client.core.util.Validators.notNullOrEmpty;
import static com.couchbase.client.java.ReactiveBinaryCollection.DEFAULT_APPEND_OPTIONS;
import static com.couchbase.client.java.ReactiveBinaryCollection.DEFAULT_DECREMENT_OPTIONS;
import static com.couchbase.client.java.ReactiveBinaryCollection.DEFAULT_INCREMENT_OPTIONS;
import static com.couchbase.client.java.ReactiveBinaryCollection.DEFAULT_PREPEND_OPTIONS;

public class AsyncBinaryCollection {

  private final Core core;
  private final CoreContext coreContext;
  private final CoreEnvironment environment;
  private final CollectionIdentifier collectionIdentifier;

  AsyncBinaryCollection(final Core core, final CoreEnvironment environment, final CollectionIdentifier collectionIdentifier) {
    this.core = core;
    this.coreContext = core.context();
    this.environment = environment;
    this.collectionIdentifier = collectionIdentifier;
  }

  public CompletableFuture<MutationResult> append(final String id, final byte[] content) {
    return append(id, content, DEFAULT_APPEND_OPTIONS);
  }

  public CompletableFuture<MutationResult> append(final String id, final byte[] content,
                                                  final AppendOptions options) {
    AppendOptions.Built opts = options.build();
    return AppendAccessor.append(
      core,
      appendRequest(id, content, options),
      id,
      opts.persistTo(),
      opts.replicateTo()
    );
  }

  AppendRequest appendRequest(final String id, final byte[] content, final AppendOptions options) {
    notNullOrEmpty(id, "Id");
    notNull(content, "Content");
    notNull(options, "AppendOptions");
    AppendOptions.Built opts = options.build();

    Duration timeout = opts.timeout().orElse(environment.timeoutConfig().kvTimeout());
    RetryStrategy retryStrategy = opts.retryStrategy().orElse(environment.retryStrategy());
    return new AppendRequest(timeout, coreContext, collectionIdentifier, retryStrategy, id, content,
      opts.cas(), opts.durabilityLevel());
  }

  public CompletableFuture<MutationResult> prepend(final String id, final byte[] content) {
    return prepend(id, content, DEFAULT_PREPEND_OPTIONS);
  }

  public CompletableFuture<MutationResult> prepend(final String id, final byte[] content,
                                                   final PrependOptions options) {
    PrependOptions.Built opts = options.build();
    return PrependAccessor.prepend(
      core,
      prependRequest(id, content, options),
      id,
      opts.persistTo(),
      opts.replicateTo()
    );
  }

  PrependRequest prependRequest(final String id, final byte[] content, final PrependOptions options) {
    notNullOrEmpty(id, "Id");
    notNull(content, "Content");
    notNull(options, "PrependOptions");
    PrependOptions.Built opts = options.build();

    Duration timeout = opts.timeout().orElse(environment.timeoutConfig().kvTimeout());
    RetryStrategy retryStrategy = opts.retryStrategy().orElse(environment.retryStrategy());
    return new PrependRequest(timeout, coreContext, collectionIdentifier, retryStrategy, id, content,
      opts.cas(), opts.durabilityLevel());
  }

  public CompletableFuture<CounterResult> increment(final String id) {
    return increment(id, DEFAULT_INCREMENT_OPTIONS);
  }

  public CompletableFuture<CounterResult> increment(final String id, final IncrementOptions options) {
    IncrementOptions.Built opts = options.build();
    return CounterAccessor.increment(
      core,
      incrementRequest(id, options),
      id,
      opts.persistTo(),
      opts.replicateTo()
    );
  }

  IncrementRequest incrementRequest(final String id, final IncrementOptions options) {
    notNullOrEmpty(id, "Id");
    notNull(options, "IncrementOptions");
    IncrementOptions.Built opts = options.build();

    Duration timeout = opts.timeout().orElse(environment.timeoutConfig().kvTimeout());
    RetryStrategy retryStrategy = opts.retryStrategy().orElse(environment.retryStrategy());
    return new IncrementRequest(timeout, coreContext, collectionIdentifier, retryStrategy, id, opts.cas(),
      opts.delta(), opts.initial(), opts.expiry(), opts.durabilityLevel());
  }

  public CompletableFuture<CounterResult> decrement(final String id) {
    return decrement(id, DEFAULT_DECREMENT_OPTIONS);
  }

  public CompletableFuture<CounterResult> decrement(final String id, final DecrementOptions options) {
    DecrementOptions.Built opts = options.build();
    return CounterAccessor.decrement(
      core,
      decrementRequest(id, options),
      id,
      opts.persistTo(),
      opts.replicateTo()
    );
  }

  DecrementRequest decrementRequest(final String id, final DecrementOptions options) {
    notNullOrEmpty(id, "Id");
    notNull(options, "DecrementOptions");
    DecrementOptions.Built opts = options.build();

    Duration timeout = opts.timeout().orElse(environment.timeoutConfig().kvTimeout());
    RetryStrategy retryStrategy = opts.retryStrategy().orElse(environment.retryStrategy());
    return new DecrementRequest(timeout, coreContext, collectionIdentifier, retryStrategy, id, opts.cas(),
      opts.delta(), opts.initial(), opts.expiry(), opts.durabilityLevel());
  }
}
