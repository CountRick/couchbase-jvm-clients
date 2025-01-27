/*
 * Copyright 2023 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.core.protostellar.kv;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.CoreKeyspace;
import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.api.kv.CoreDurability;
import com.couchbase.client.core.api.kv.CoreEncodedContent;
import com.couchbase.client.core.cnc.CbTracing;
import com.couchbase.client.core.cnc.RequestSpan;
import com.couchbase.client.core.cnc.TracingIdentifiers;
import com.couchbase.client.core.deps.com.google.protobuf.ByteString;
import com.couchbase.client.core.endpoint.http.CoreCommonOptions;
import com.couchbase.client.core.protostellar.CoreProtostellarUtil;
import com.couchbase.client.core.protostellar.ProtostellarKeyValueRequest;
import com.couchbase.client.core.protostellar.ProtostellarRequest;
import com.couchbase.client.protostellar.kv.v1.InsertRequest;
import com.couchbase.client.protostellar.kv.v1.ReplaceRequest;
import com.couchbase.client.protostellar.kv.v1.UpsertRequest;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static com.couchbase.client.core.api.kv.CoreKvParamValidators.validateGetParams;
import static com.couchbase.client.core.api.kv.CoreKvParamValidators.validateInsertParams;
import static com.couchbase.client.core.api.kv.CoreKvParamValidators.validateRemoveParams;
import static com.couchbase.client.core.api.kv.CoreKvParamValidators.validateReplaceParams;
import static com.couchbase.client.core.api.kv.CoreKvParamValidators.validateUpsertParams;
import static com.couchbase.client.core.protostellar.CoreProtostellarUtil.convertFromFlags;
import static com.couchbase.client.core.protostellar.CoreProtostellarUtil.createSpan;

/**
 * For creating Protostellar GRPC KV requests.
 */
@Stability.Internal
public class CoreProtostellarKeyValueRequests {
  private CoreProtostellarKeyValueRequests() {
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.GetRequest> getRequest(Core core,
                                                                                                   CoreCommonOptions opts,
                                                                                                   CoreKeyspace keyspace,
                                                                                                   String key,
                                                                                                   List<String> projections,
                                                                                                   boolean withExpiry) {
    validateGetParams(opts, key, projections, withExpiry);

    Duration timeout = CoreProtostellarUtil.kvTimeout(opts.timeout(), core);
    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.GetRequest> out = new ProtostellarKeyValueRequest<>(core,
      keyspace,
      key,
      CoreDurability.NONE,
      TracingIdentifiers.SPAN_REQUEST_KV_GET,
      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_GET, CoreDurability.NONE, opts.parentSpan().orElse(null)),
      timeout,
      true,
      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
      opts.clientContext());

    out.request(com.couchbase.client.protostellar.kv.v1.GetRequest.newBuilder()
      .setBucketName(keyspace.bucket())
      .setScopeName(keyspace.scope())
      .setCollectionName(keyspace.collection())
      .setKey(key)
      .build());

    if (!projections.isEmpty() || withExpiry) {
      throw new UnsupportedOperationException("Projections and withExpiry are not yet supported with Protostellar, but will be");
    }

    return out;
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.GetAndLockRequest> getAndLockRequest(Core core,
                                                                                                                 CoreCommonOptions opts,
                                                                                                                 CoreKeyspace keyspace,
                                                                                                                 String key,
                                                                                                                 Duration lockTime) {
    throw new UnsupportedOperationException("This operation is implemented in Protostellar but not STG currently");

//    validateGetAndLockParams(opts, key, lockTime);
//
//    Duration timeout = CoreProtostellarUtil.kvTimeout(opts.timeout(), core);
//    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.GetAndLockRequest> out = new ProtostellarKeyValueRequest<>(core,
//      keyspace,
//      key,
//      CoreDurability.NONE,
//      TracingIdentifiers.SPAN_REQUEST_KV_GET_AND_LOCK,
//      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_GET_AND_LOCK, CoreDurability.NONE, opts.parentSpan().orElse(null)),
//      timeout,
//      true,
//      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
//      opts.clientContext());
//
//    out.request(com.couchbase.client.protostellar.kv.v1.GetAndLockRequest.newBuilder()
//      .setBucketName(keyspace.bucket())
//      .setScopeName(keyspace.scope())
//      .setCollectionName(keyspace.collection())
//      .setKey(key)
//      .setLockTime((int) lockTime.toMillis())
//      .build());
//
//    return out;
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.GetAndTouchRequest> getAndTouchRequest(Core core,
                                                                                                                   CoreCommonOptions opts,
                                                                                                                   CoreKeyspace keyspace,
                                                                                                                   String key,
                                                                                                                   long expiration) {
    throw new UnsupportedOperationException("This operation is implemented in Protostellar but not STG currently");

//    validateGetAndTouchParams(opts, key, expiration);
//
//    Duration timeout = CoreProtostellarUtil.kvTimeout(opts.timeout(), core);
//    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.GetAndTouchRequest> out = new ProtostellarKeyValueRequest<>(core,
//      keyspace,
//      key,
//      CoreDurability.NONE,
//      TracingIdentifiers.SPAN_REQUEST_KV_GET_AND_TOUCH,
//      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_GET_AND_TOUCH, CoreDurability.NONE, opts.parentSpan().orElse(null)),
//      timeout,
//      true,
//      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
//      opts.clientContext());
//
//    out.request(com.couchbase.client.protostellar.kv.v1.GetAndTouchRequest.newBuilder()
//      .setBucketName(keyspace.bucket())
//      .setScopeName(keyspace.scope())
//      .setCollectionName(keyspace.collection())
//      .setKey(key)
//      .setExpiry(convertExpiry(expiration))
//      .build());
//
//    return out;
  }

  public static ProtostellarRequest<InsertRequest> insertRequest(Core core,
                                                                 CoreKeyspace keyspace,
                                                                 CoreCommonOptions opts,
                                                                 String key,
                                                                 Supplier<CoreEncodedContent> content,
                                                                 CoreDurability durability,
                                                                 long expiry) {
    validateInsertParams(opts, key, content, durability, expiry);

    Duration timeout = CoreProtostellarUtil.kvDurableTimeout(opts.timeout(), durability, core);
    ProtostellarRequest<InsertRequest> out = new ProtostellarKeyValueRequest<>(core,
      keyspace,
      key,
      durability,
      TracingIdentifiers.SPAN_REQUEST_KV_INSERT,
      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_INSERT, durability, opts.parentSpan().orElse(null)),
      timeout,
      false,
      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
      opts.clientContext());

    CoreEncodedContent encoded = encodedContent(core, content, out);

    InsertRequest.Builder request = InsertRequest.newBuilder()
      .setBucketName(keyspace.bucket())
      .setScopeName(keyspace.scope())
      .setCollectionName(keyspace.collection())
      .setKey(key)
      .setContent(ByteString.copyFrom(encoded.encoded()))
      .setContentType(convertFromFlags(encoded.flags()));

    if (expiry != 0) {
      request.setExpiry(CoreProtostellarUtil.convertExpiry(expiry));
    }
    if (!durability.isNone()) {
      request.setDurabilityLevel(CoreProtostellarUtil.convert(durability));
    }

    out.request(request.build());

    return out;
  }

  public static ProtostellarRequest<ReplaceRequest> replaceRequest(Core core,
                                                                   CoreKeyspace keyspace,
                                                                   CoreCommonOptions opts,
                                                                   String key,
                                                                   Supplier<CoreEncodedContent> content,
                                                                   long cas,
                                                                   CoreDurability durability,
                                                                   long expiry,
                                                                   boolean preserveExpiry) {
    validateReplaceParams(opts, key, content, cas, durability, expiry, preserveExpiry);

    Duration timeout = CoreProtostellarUtil.kvDurableTimeout(opts.timeout(), durability, core);
    ProtostellarRequest<ReplaceRequest> out = new ProtostellarKeyValueRequest<>(core,
      keyspace,
      key,
      durability,
      TracingIdentifiers.SPAN_REQUEST_KV_REPLACE,
      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_REPLACE, durability, opts.parentSpan().orElse(null)),
      timeout,
      false,
      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
      opts.clientContext());

    CoreEncodedContent encoded = encodedContent(core, content, out);

    ReplaceRequest.Builder request = ReplaceRequest.newBuilder()
      .setBucketName(keyspace.bucket())
      .setScopeName(keyspace.scope())
      .setCollectionName(keyspace.collection())
      .setKey(key)
      .setCas(cas)
      .setContent(ByteString.copyFrom(encoded.encoded()))
      .setContentType(convertFromFlags(encoded.flags()));

    if (expiry != 0) {
      request.setExpiry(CoreProtostellarUtil.convertExpiry(expiry));
    }
    if (!durability.isNone()) {
      request.setDurabilityLevel(CoreProtostellarUtil.convert(durability));
    }

    if (preserveExpiry) {
      throw new UnsupportedOperationException("preserveExpiry is not yet supported with Protostellar, but will be");
    }

    out.request(request.build());

    return out;
  }

  public static ProtostellarRequest<UpsertRequest> upsertRequest(Core core,
                                                                 CoreKeyspace keyspace,
                                                                 CoreCommonOptions opts,
                                                                 String key,
                                                                 Supplier<CoreEncodedContent> content,
                                                                 CoreDurability durability,
                                                                 long expiry,
                                                                 boolean preserveExpiry) {
    validateUpsertParams(opts, key, content, durability, expiry, preserveExpiry);

    Duration timeout = CoreProtostellarUtil.kvDurableTimeout(opts.timeout(), durability, core);
    ProtostellarRequest<UpsertRequest> out = new ProtostellarKeyValueRequest<>(core,
      keyspace,
      key,
      durability,
      TracingIdentifiers.SPAN_REQUEST_KV_UPSERT,
      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_UPSERT, durability, opts.parentSpan().orElse(null)),
      timeout,
      false,
      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
      opts.clientContext());

    CoreEncodedContent encoded = encodedContent(core, content, out);

    UpsertRequest.Builder request = UpsertRequest.newBuilder()
      .setBucketName(keyspace.bucket())
      .setScopeName(keyspace.scope())
      .setCollectionName(keyspace.collection())
      .setKey(key)
      .setContent(ByteString.copyFrom(encoded.encoded()))
      .setContentType(convertFromFlags(encoded.flags()));

    if (expiry != 0) {
      request.setExpiry(CoreProtostellarUtil.convertExpiry(expiry));
    }
    if (!durability.isNone()) {
      request.setDurabilityLevel(CoreProtostellarUtil.convert(durability));
    }

    if (preserveExpiry) {
      throw new UnsupportedOperationException("preserveExpiry is not yet supported with Protostellar, but will be");
    }

    out.request(request.build());

    return out;
  }

  private static CoreEncodedContent encodedContent(Core core, Supplier<CoreEncodedContent> content, ProtostellarRequest<?> out) {
    RequestSpan encodeSpan = CbTracing.newSpan(core.context(), TracingIdentifiers.SPAN_REQUEST_ENCODING, out.span());
    long start = System.nanoTime();
    CoreEncodedContent encoded;
    try {
      encoded = content.get();
    } finally {
      encodeSpan.end();
    }

    out.encodeLatency(System.nanoTime() - start);
    return encoded;
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.RemoveRequest> removeRequest(Core core,
                                                                                                         CoreKeyspace keyspace,
                                                                                                         CoreCommonOptions opts,
                                                                                                         String key,
                                                                                                         long cas,
                                                                                                         CoreDurability durability) {
    validateRemoveParams(opts, key, cas, durability);
    Duration timeout = CoreProtostellarUtil.kvDurableTimeout(opts.timeout(), durability, core);
    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.RemoveRequest> out = new ProtostellarKeyValueRequest<>(core,
      keyspace,
      key,
      durability,
      TracingIdentifiers.SPAN_REQUEST_KV_REMOVE,
      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_REMOVE, durability, opts.parentSpan().orElse(null)),
      timeout,
      false,
      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
      opts.clientContext());

    com.couchbase.client.protostellar.kv.v1.RemoveRequest.Builder request = com.couchbase.client.protostellar.kv.v1.RemoveRequest.newBuilder()
      .setBucketName(keyspace.bucket())
      .setScopeName(keyspace.scope())
      .setCollectionName(keyspace.collection())
      .setKey(key)
      .setCas(cas);

    if (!durability.isNone()) {
      request.setDurabilityLevel(CoreProtostellarUtil.convert(durability));
    }

    out.request(request.build());
    return out;
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.ExistsRequest> existsRequest(Core core,
                                                                                                         CoreKeyspace keyspace,
                                                                                                         CoreCommonOptions opts,
                                                                                                         String key) {
    throw new UnsupportedOperationException("This operation is implemented in Protostellar but not STG currently");

//    validateExistsParams(opts, key);
//
//    Duration timeout = CoreProtostellarUtil.kvTimeout(opts.timeout(), core);
//    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.ExistsRequest> out = new ProtostellarKeyValueRequest<>(core,
//      keyspace,
//      key,
//      CoreDurability.NONE,
//      TracingIdentifiers.SPAN_REQUEST_KV_EXISTS,
//      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_EXISTS, CoreDurability.NONE, opts.parentSpan().orElse(null)),
//      timeout,
//      true,
//      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
//      opts.clientContext());
//
//    com.couchbase.client.protostellar.kv.v1.ExistsRequest.Builder request = com.couchbase.client.protostellar.kv.v1.ExistsRequest.newBuilder()
//      .setBucketName(keyspace.bucket())
//      .setScopeName(keyspace.scope())
//      .setCollectionName(keyspace.collection())
//      .setKey(key);
//
//    out.request(request.build());
//    return out;
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.TouchRequest> touchRequest(Core core,
                                                                                                       CoreKeyspace keyspace,
                                                                                                       CoreCommonOptions opts,
                                                                                                       String key,
                                                                                                       long expiry) {
    throw new UnsupportedOperationException("This operation is implemented in Protostellar but not STG currently");

//    validateTouchParams(opts, key, expiry);
//
//    Duration timeout = CoreProtostellarUtil.kvTimeout(opts.timeout(), core);
//    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.TouchRequest> out = new ProtostellarKeyValueRequest<>(core,
//      keyspace,
//      key,
//      CoreDurability.NONE,
//      TracingIdentifiers.SPAN_REQUEST_KV_TOUCH,
//      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_TOUCH, CoreDurability.NONE, opts.parentSpan().orElse(null)),
//      timeout,
//      true,
//      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
//      opts.clientContext());
//
//    com.couchbase.client.protostellar.kv.v1.TouchRequest.Builder request = com.couchbase.client.protostellar.kv.v1.TouchRequest.newBuilder()
//      .setBucketName(keyspace.bucket())
//      .setScopeName(keyspace.scope())
//      .setCollectionName(keyspace.collection())
//      .setExpiry(convertExpiry(expiry))
//      .setKey(key);
//
//    out.request(request.build());
//    return out;
  }

  public static ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.UnlockRequest> unlockRequest(Core core,
                                                                                                         CoreKeyspace keyspace,
                                                                                                         CoreCommonOptions opts,
                                                                                                         String key,
                                                                                                         long cas) {
    throw new UnsupportedOperationException("This operation is implemented in Protostellar but not STG currently");

//    validateUnlockParams(opts, key, cas, keyspace.toCollectionIdentifier());
//
//    Duration timeout = CoreProtostellarUtil.kvTimeout(opts.timeout(), core);
//    ProtostellarRequest<com.couchbase.client.protostellar.kv.v1.UnlockRequest> out = new ProtostellarKeyValueRequest<>(core,
//      keyspace,
//      key,
//      CoreDurability.NONE,
//      TracingIdentifiers.SPAN_REQUEST_KV_UNLOCK,
//      createSpan(core, TracingIdentifiers.SPAN_REQUEST_KV_UNLOCK, CoreDurability.NONE, opts.parentSpan().orElse(null)),
//      timeout,
//      true,
//      opts.retryStrategy().orElse(core.context().environment().retryStrategy()),
//      opts.clientContext());
//
//    com.couchbase.client.protostellar.kv.v1.UnlockRequest.Builder request = com.couchbase.client.protostellar.kv.v1.UnlockRequest.newBuilder()
//      .setBucketName(keyspace.bucket())
//      .setScopeName(keyspace.scope())
//      .setCollectionName(keyspace.collection())
//      .setCas(cas)
//      .setKey(key);
//
//    out.request(request.build());
//    return out;
  }
}
