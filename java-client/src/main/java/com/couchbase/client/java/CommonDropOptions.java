/*
 * Copyright 2023 Couchbase, Inc.
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
package com.couchbase.client.java;

public abstract class CommonDropOptions<SELF extends CommonDropOptions<SELF>> extends CommonOptions<SELF> {
  private boolean ignoreIfNotExists;

  /**
   * If the bucket exists, an exception will be thrown unless this is set to true.
   */
  public SELF ignoreIfNotExists(boolean ignore) {
    this.ignoreIfNotExists = ignore;
    return self();
  }

  protected abstract class BuiltDropOptions extends BuiltCommonOptions {

    public boolean ignoreIfNotExists() {
      return ignoreIfNotExists;
    }
  }
}
