/*
 * Copyright 2021 Couchbase, Inc.
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
package com.couchbase.client.java.manager.eventing;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.java.CommonOptions;

/**
 * Allows to customize the behavior of the eventing functions drop operation.
 */
public class DropFunctionOptions extends CommonOptions<DropFunctionOptions> {
  private DropFunctionOptions() {
  }

  /**
   * Creates a new instance with default values.
   *
   * @return the instantiated default options.
   */
  public static DropFunctionOptions dropFunctionOptions() {
    return new DropFunctionOptions();
  }

  @Stability.Internal
  public Built build() {
    return new Built();
  }

  public class Built extends BuiltCommonOptions {
    Built() { }
  }

}
