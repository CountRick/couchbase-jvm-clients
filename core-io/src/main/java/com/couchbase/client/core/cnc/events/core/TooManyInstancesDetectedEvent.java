/*
 * Copyright (c) 2022 Couchbase, Inc.
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

package com.couchbase.client.core.cnc.events.core;

import com.couchbase.client.core.cnc.AbstractEvent;

import java.time.Duration;

/**
 * This event is raised if too many instances of the SDK are created as configured.
 */
public class TooManyInstancesDetectedEvent extends AbstractEvent {

  private final String description;

  public TooManyInstancesDetectedEvent(final String description) {
    super(Severity.WARN, Category.CORE, Duration.ZERO, null);
    this.description = description;
  }

  @Override
  public String description() {
    return description;
  }

}
