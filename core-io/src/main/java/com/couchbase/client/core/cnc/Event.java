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

package com.couchbase.client.core.cnc;

import java.time.Duration;
import java.time.Instant;

/**
 * The parent interface for all events pushed through the command and
 * control system.
 */
public interface Event {

  /**
   * Returns the value of {@code System.nanoTime()} when the event was created.
   *
   * @deprecated Please use {@link #created} instead.
   */
  @Deprecated
  long createdAt();

  /**
   * Returns the creation timestamp of this event.
   */
  Instant created();

  /**
   * The Severity of this event.
   *
   * @return the event severity.
   */
  Severity severity();

  /**
   * The Category of this event.
   *
   * @return the event category.
   */
  String category();

  /**
   * Returns the duration of this event.
   *
   * @return the duration of the even, 0 if not set.
   */
  Duration duration();

  /**
   * The context this event is referencing.
   *
   * @return the referencing context.
   */
  Context context();

  /**
   * A textual description with more information about the event.
   *
   * @return the description, if set.
   */
  String description();

  /**
   * If present, holds the cause for this event. Usually present if raised because of an excetion.
   *
   * @return the throwable if present.
   */
  default Throwable cause() {
    return null;
  }

  /**
   * Describes the severity of any given event.
   */
  enum Severity {
    /**
     * Verbose information used to trace certain actual
     * data throughout the system.
     */
    VERBOSE,

    /**
     * Information that guide debugging and in-depth
     * troubleshooting.
     */
    DEBUG,

    /**
     * Should rely non-critical information.
     */
    INFO,

    /**
     * Indicates that a component is in a non-ideal state
     * and that something could escalate into an error
     * potentially.
     */
    WARN,

    /**
     * Critical errors that require immediate attention and/or
     * problems which are not recoverable by the system itself.
     */
    ERROR,

    /**
     * Events that are created which deal with request and response tracing
     * (not to be confused with TRACE logging which is on purpose called
     * VERBOSE here so that they are not easily confused).
     */
    TRACING
  }

  String CATEGORY_PREFIX = "com.couchbase.";

  /**
   * Describes the category of any given event.
   */
  enum Category {
    /**
     * Represents an event from the IO subsystem.
     */
    IO(CATEGORY_PREFIX + "io"),
    /**
     * Represents an event from the Endpoint layer.
     */
    ENDPOINT(CATEGORY_PREFIX + "endpoint"),
    /**
     * Represents an event around a specific request instance.
     */
    REQUEST(CATEGORY_PREFIX + "request"),
    /**
     * Represents an event that concerns the JVM/OS/system.
     */
    SYSTEM(CATEGORY_PREFIX + "system"),
    /**
     * Represents an event from the Service layer.
     */
    SERVICE(CATEGORY_PREFIX + "service"),
    /**
     * Represents an event from the Node layer.
     */
    NODE(CATEGORY_PREFIX + "node"),
    /**
     * Represents an event from the config subsystem.
     */
    CONFIG(CATEGORY_PREFIX + "config"),
    /**
     * Represents an event from the upper level core subsystem.
     */
    CORE(CATEGORY_PREFIX + "core"),
    /**
     * Represents event that come from the tracing subsystem.
     */
    TRACING(CATEGORY_PREFIX + "tracing"),
    /**
     * Represents events that comes from the metrics subsystem.
     */
    METRICS(CATEGORY_PREFIX + "metrics");

    private final String path;

    Category(String path) {
      this.path = path;
    }

    public String path() {
      return path;
    }
  }

}
