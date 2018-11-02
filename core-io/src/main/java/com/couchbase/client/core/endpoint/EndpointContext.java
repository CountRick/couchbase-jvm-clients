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

package com.couchbase.client.core.endpoint;

import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.io.NetworkAddress;

import java.util.Map;

public class EndpointContext extends CoreContext {

  /**
   * The hostname of this endpoint.
   */
  private NetworkAddress remoteHostname;

  /**
   * The port of this endpoint.
   */
  private int remotePort;

  /**
   * Creates a new {@link EndpointContext}.
   *
   * @param ctx the parent context to use.
   * @param remoteHostname the remote hostname.
   * @param remotePort the remote port.
   */
  public EndpointContext(CoreContext ctx, NetworkAddress remoteHostname, int remotePort) {
    super(ctx.id(), ctx.environment());
    this.remoteHostname = remoteHostname;
    this.remotePort = remotePort;
  }

  @Override
  protected void injectExportableParams(final Map<String, Object> input) {
    super.injectExportableParams(input);
    input.put("remote", remoteHostname().nameOrAddress() + ":" + remotePort());
  }

  public NetworkAddress remoteHostname() {
    return remoteHostname;
  }

  public int remotePort() {
    return remotePort;
  }
}
