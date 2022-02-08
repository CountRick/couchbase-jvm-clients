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

package com.couchbase.client.java.manager.analytics;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.java.CommonOptions;

import java.util.Optional;

/**
 * Customizes how an index is created.
 */
public class CreateIndexAnalyticsOptions extends CommonOptions<CreateIndexAnalyticsOptions> {

  private boolean ignoreIfExists;
  private Optional<String> dataverseName = Optional.empty();

  private CreateIndexAnalyticsOptions() {
  }

  /**
   * Creates a new instance with default values.
   *
   * @return the instantiated default options.
   */
  public static CreateIndexAnalyticsOptions createIndexAnalyticsOptions() {
    return new CreateIndexAnalyticsOptions();
  }

  /**
   * Ignore the create operation if the index exists.
   *
   * @param ignore if true no exception will be thrown if the index already exists.
   * @return this {@link CreateIndexAnalyticsOptions} for chaining purposes.
   */
  public CreateIndexAnalyticsOptions ignoreIfExists(final boolean ignore) {
    this.ignoreIfExists = ignore;
    return this;
  }

  /**
   * The name of the dataverse in which the index exists.
   *
   * @param dataverseName the name of the dataverse.
   * @return this {@link CreateIndexAnalyticsOptions} for chaining purposes.
   */
  public CreateIndexAnalyticsOptions dataverseName(final String dataverseName) {
    this.dataverseName = Optional.ofNullable(dataverseName);
    return this;
  }

  @Stability.Internal
  public Built build() {
    return new Built();
  }

  public class Built extends BuiltCommonOptions {

    Built() { }

    public boolean ignoreIfExists() {
      return ignoreIfExists;
    }

    public Optional<String> dataverseName() {
      return dataverseName;
    }
  }
}
