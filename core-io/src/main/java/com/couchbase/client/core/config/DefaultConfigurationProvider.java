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

package com.couchbase.client.core.config;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.config.loader.KeyValueLoader;
import com.couchbase.client.core.config.loader.ClusterManagerLoader;
import com.couchbase.client.core.env.SeedNode;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;


/**
 * The standard {@link ConfigurationProvider} that is used in a regular deployment.
 *
 * @since 1.0.0
 */
public class DefaultConfigurationProvider implements ConfigurationProvider {

  private final Core core;
  private final DirectProcessor<ClusterConfig> configs;
  private final FluxSink<ClusterConfig> configsSink;
  private final ClusterConfig currentConfig;

  public DefaultConfigurationProvider(Core core) {
    this.configs = DirectProcessor.create();
    this.configsSink = configs.sink();
    currentConfig = new ClusterConfig();
    this.core = core;
  }

  @Override
  public Flux<ClusterConfig> configs() {
    return configs;
  }

  @Override
  public ClusterConfig config() {
    return currentConfig;
  }

  @Override
  public Mono<Void> openBucket(String name) {
    KeyValueLoader keyValueLoader = new KeyValueLoader(core);
    ClusterManagerLoader clusterManagerLoader = new ClusterManagerLoader(core);

    // TODO this is a hack and not the proper functionality

    SeedNode seedNode = core.context().environment().seedNodes().iterator().next();
    return keyValueLoader
      .load(seedNode.getAddress(),seedNode.kvPort().orElse(11210), name)
      .onErrorResume(throwable -> clusterManagerLoader.load(seedNode.getAddress(), seedNode.httpPort().orElse(8091), name))
      .flatMap(config -> {
        currentConfig.setBucketConfig(name, config);
        configs.onNext(currentConfig);
        return Mono.empty();
      });
  }

  @Override
  public Mono<Void> closeBucket(String name) {
    return null;
  }

  @Override
  public Mono<Void> shutdown() {
    return null;
  }

}
