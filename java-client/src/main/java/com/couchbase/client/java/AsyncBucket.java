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
import com.couchbase.client.java.env.CouchbaseEnvironment;

public class AsyncBucket {

  private final String name;
  private final CouchbaseEnvironment environment;
  private final Core core;

  AsyncBucket(String name, Core core, CouchbaseEnvironment environment) {
    this.core = core;
    this.environment = environment;
    this.name = name;
  }

  public AsyncCollection defaultCollection() {
    return collection(null, null);
  }

  public AsyncCollection collection(final String name) {
    return collection(name, null);
  }

  public AsyncCollection collection(final String name, final String scope) {
    return new AsyncCollection(name, scope, core, environment);
  }

  CouchbaseEnvironment environment() {
    return environment;
  }

  Core core() {
    return core;
  }
}
