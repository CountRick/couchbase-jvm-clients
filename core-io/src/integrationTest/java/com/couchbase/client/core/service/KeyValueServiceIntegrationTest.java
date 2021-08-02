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

package com.couchbase.client.core.service;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.client.core.msg.kv.NoopRequest;
import com.couchbase.client.core.msg.kv.NoopResponse;
import com.couchbase.client.core.util.CoreIntegrationTest;
import com.couchbase.client.test.Services;
import com.couchbase.client.test.TestNodeConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.test.Util.waitUntilCondition;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyValueServiceIntegrationTest extends CoreIntegrationTest {

  private static CoreEnvironment env;
  private static Core core;

  @BeforeAll
  static void beforeAll() {
    env = environment().build();
    core = Core.create(env, authenticator(), seedNodes());
  }

  @AfterAll
  static void afterAll() {
    core.shutdown().block();
    env.shutdown();
  }

  /**
   * The most simplistic end-to-end test for a KV service.
   *
   * <p>This integration test connects to a node and then performs a NOOP and
   * waits for a successful response.</p>
   *
   * @throws Exception if waiting on the response fails.
   */
  @Test
  void connectNoopAndDisconnect() throws Exception {
    TestNodeConfig node = config().nodes().get(0);

    KeyValueService service = new KeyValueService(
      KeyValueServiceConfig.builder().build(),
      core.context(),
      node.hostname(),
      node.ports().get(Services.KV),
      Optional.of(config().bucketname()),
      core.context().authenticator()
    );

    service.connect();
    waitUntilCondition(() -> service.state() == ServiceState.CONNECTED);

    NoopRequest request = new NoopRequest(kvTimeout, core.context(), null, CollectionIdentifier.fromDefault(config().bucketname()));
    assertTrue(request.id() > 0);
    service.send(request);

    NoopResponse response = request.response().get(1, TimeUnit.SECONDS);
    assertTrue(response.status().success());

    assertTrue(request.context().dispatchLatency() > 0);

    service.disconnect();
    waitUntilCondition(() -> service.state() == ServiceState.DISCONNECTED);
  }
}
