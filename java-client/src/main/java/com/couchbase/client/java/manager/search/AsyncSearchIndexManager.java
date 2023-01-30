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

package com.couchbase.client.java.manager.search;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.cnc.CbTracing;
import com.couchbase.client.core.cnc.RequestSpan;
import com.couchbase.client.core.cnc.TracingIdentifiers;
import com.couchbase.client.core.deps.com.fasterxml.jackson.core.type.TypeReference;
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpHeaderNames;
import com.couchbase.client.core.endpoint.http.CoreHttpClient;
import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.FeatureNotAvailableException;
import com.couchbase.client.core.error.IndexNotFoundException;
import com.couchbase.client.core.json.Mapper;
import com.couchbase.client.core.msg.RequestTarget;
import com.couchbase.client.java.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.couchbase.client.core.endpoint.http.CoreHttpPath.path;
import static com.couchbase.client.core.util.CbThrowables.hasCause;
import static com.couchbase.client.core.util.CbThrowables.throwIfUnchecked;
import static com.couchbase.client.core.util.UrlQueryStringBuilder.urlEncode;
import static com.couchbase.client.core.util.Validators.notNull;
import static com.couchbase.client.core.util.Validators.notNullOrEmpty;
import static com.couchbase.client.java.manager.search.AllowQueryingSearchIndexOptions.allowQueryingSearchIndexOptions;
import static com.couchbase.client.java.manager.search.AnalyzeDocumentOptions.analyzeDocumentOptions;
import static com.couchbase.client.java.manager.search.DisallowQueryingSearchIndexOptions.disallowQueryingSearchIndexOptions;
import static com.couchbase.client.java.manager.search.DropSearchIndexOptions.dropSearchIndexOptions;
import static com.couchbase.client.java.manager.search.FreezePlanSearchIndexOptions.freezePlanSearchIndexOptions;
import static com.couchbase.client.java.manager.search.GetAllSearchIndexesOptions.getAllSearchIndexesOptions;
import static com.couchbase.client.java.manager.search.GetIndexedSearchIndexOptions.getIndexedSearchIndexOptions;
import static com.couchbase.client.java.manager.search.GetSearchIndexOptions.getSearchIndexOptions;
import static com.couchbase.client.java.manager.search.PauseIngestSearchIndexOptions.pauseIngestSearchIndexOptions;
import static com.couchbase.client.java.manager.search.ResumeIngestSearchIndexOptions.resumeIngestSearchIndexOptions;
import static com.couchbase.client.java.manager.search.UnfreezePlanSearchIndexOptions.unfreezePlanSearchIndexOptions;
import static com.couchbase.client.java.manager.search.UpsertSearchIndexOptions.upsertSearchIndexOptions;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The {@link AsyncSearchIndexManager} allows to manage search index structures in a couchbase cluster.
 *
 * @since 3.0.0
 */
public class AsyncSearchIndexManager {

  private final Core core;
  private final CoreHttpClient searchHttpClient;

  public AsyncSearchIndexManager(final Core core) {
    this.core = core;
    this.searchHttpClient = core.httpClient(RequestTarget.search());
  }

  private static String indexesPath() {
    return "/api/index";
  }

  private static String indexPath(final String indexName) {
    return indexesPath() + "/" + urlEncode(indexName);
  }

  private static String indexCountPath(final String indexName) {
    return indexPath(indexName) + "/count";
  }

  private static String analyzeDocumentPath(final String indexName) {
    return indexPath(indexName) + "/analyzeDoc";
  }

  private static String pauseIngestPath(final String indexName) {
    return indexPath(indexName) + "/ingestControl/pause";
  }

  private static String resumeIngestPath(final String indexName) {
    return indexPath(indexName) + "/ingestControl/resume";
  }

  private static String allowQueryingPath(final String indexName) {
    return indexPath(indexName) + "/queryControl/allow";
  }

  private static String disallowQueryingPath(final String indexName) {
    return indexPath(indexName) + "/queryControl/disallow";
  }

  private static String freezePlanPath(final String indexName) {
    return indexPath(indexName) + "/planFreezeControl/freeze";
  }

  private static String unfreezePlanPath(final String indexName) {
    return indexPath(indexName) + "/planFreezeControl/unfreeze";
  }

  /**
   * Fetches an index from the server if it exists.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} the found index once complete.
   */
  public CompletableFuture<SearchIndex> getIndex(final String name) {
    return getIndex(name, getSearchIndexOptions());
  }

  /**
   * Fetches an index from the server if it exists.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} the found index once complete.
   */
  public CompletableFuture<SearchIndex> getIndex(final String name, GetSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    RequestSpan span = CbTracing.newSpan(
        core.context(),
        TracingIdentifiers.SPAN_REQUEST_MS_GET_INDEX,
        options.build().parentSpan().orElse(null)
    );

    return getAllIndexes(getAllSearchIndexesOptions().parentSpan(span))
      .thenApply(indexes -> indexes.stream()
        .filter(i -> i.name().equals(name))
        .findFirst().orElseThrow(() -> new IndexNotFoundException(name)))
      .whenComplete((r, t) -> span.end());
  }

  /**
   * Fetches all indexes from the server.
   *
   * @return a {@link CompletableFuture} with all index definitions once complete.
   */
  public CompletableFuture<List<SearchIndex>> getAllIndexes() {
    return getAllIndexes(getAllSearchIndexesOptions());
  }

  /**
   * Fetches all indexes from the server.
   *
   * @return a {@link CompletableFuture} with all index definitions once complete.
   */
  public CompletableFuture<List<SearchIndex>> getAllIndexes(final GetAllSearchIndexesOptions options) {
    return searchHttpClient.get(path(indexesPath()), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_GET_ALL_INDEXES)
      .exec(core)
      .thenApply(response -> {
        JsonNode rootNode = Mapper.decodeIntoTree(response.content());
        JsonNode indexDefs = rootNode.get("indexDefs").get("indexDefs");
        Map<String, SearchIndex> indexes = Mapper.convertValue(
          indexDefs,
          new TypeReference<Map<String, SearchIndex>>() {}
        );
        return indexes == null ? Collections.emptyList() : new ArrayList<>(indexes.values());
      });
  }

  /**
   * Retrieves the number of documents that have been indexed for an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} with the indexed documents count once complete.
   */
  public CompletableFuture<Long> getIndexedDocumentsCount(final String name) {
    return getIndexedDocumentsCount(name, getIndexedSearchIndexOptions());
  }

  /**
   * Retrieves the number of documents that have been indexed for an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} with the indexed documents count once complete.
   */
  public CompletableFuture<Long> getIndexedDocumentsCount(final String name, final GetIndexedSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.get(path(indexCountPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_GET_IDX_DOC_COUNT)
      .exec(core)
      .thenApply(response -> {
        JsonNode rootNode = Mapper.decodeIntoTree(response.content());
        return rootNode.get("count").asLong();
      });
  }

  /**
   * Creates, or updates, an index.
   *
   * @param index the index definition to upsert.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> upsertIndex(final SearchIndex index) {
    return upsertIndex(index, upsertSearchIndexOptions());
  }

  /**
   * Creates, or updates, an index.
   *
   * @param index the index definition to upsert.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> upsertIndex(final SearchIndex index, final UpsertSearchIndexOptions options) {
    notNull(index, "Search Index");

    return searchHttpClient.put(path(indexPath(index.name())), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_UPSERT_INDEX)
      .json(index.toJson().getBytes(UTF_8))
      .header(HttpHeaderNames.CACHE_CONTROL, "no-cache")
      .exec(core)
      .thenApply(response -> null);
  }

  /**
   * Drops an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> dropIndex(final String name) {
    return dropIndex(name, dropSearchIndexOptions());
  }

  /**
   * Drops an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> dropIndex(final String name, final DropSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");
    DropSearchIndexOptions.Built bltOptions = options.build();
    return searchHttpClient.delete(path(indexPath(name)), bltOptions)
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_DROP_INDEX)
      .exec(core)
      .exceptionally(t -> {
        if (bltOptions.ignoreIfNotExists() && hasCause(t, IndexNotFoundException.class)) {
          return null;
        }
        throwIfUnchecked(t);
        throw new RuntimeException(t);
      })
      .thenApply(response -> null);
  }

  /**
   * Allows to see how a document is analyzed against a specific index.
   *
   * @param name the name of the search index.
   * @param document the document to analyze.
   * @return a {@link CompletableFuture} with analyzed document parts once complete.
   */
  public CompletableFuture<List<JsonObject>> analyzeDocument(final String name, final JsonObject document) {
    return analyzeDocument(name, document, analyzeDocumentOptions());
  }

  /**
   * Allows to see how a document is analyzed against a specific index.
   *
   * @param name the name of the search index.
   * @param document the document to analyze.
   * @return a {@link CompletableFuture} with analyzed document parts once complete.
   */
  public CompletableFuture<List<JsonObject>> analyzeDocument(final String name, final JsonObject document,
                                                             final AnalyzeDocumentOptions options) {
    notNullOrEmpty(name, "Search Index Name");
    notNull(document, "Document");

    return searchHttpClient.post(path(analyzeDocumentPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_ANALYZE_DOCUMENT)
      .json(Mapper.encodeAsBytes(document.toMap()))
      .exec(core)
      .exceptionally(throwable -> {
        if (throwable.getMessage().contains("Page not found")) {
          throw new FeatureNotAvailableException("Document analysis is not available on this server version!");
        } else if (throwable instanceof RuntimeException) {
          throw (RuntimeException) throwable;
        } else {
          throw new CouchbaseException("Failed to analyze search document", throwable);
        }
      })
      .thenApply(response -> {
        JsonNode rootNode = Mapper.decodeIntoTree(response.content());
        List<Map<String, Object>> analyzed = Mapper.convertValue(
          rootNode.get("analyzed"),
          new TypeReference<List<Map<String, Object>>>() {}
        );
        return analyzed.stream().filter(Objects::nonNull).map(JsonObject::from).collect(Collectors.toList());
      });
  }

  /**
   * Pauses updates and maintenance for an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> pauseIngest(final String name) {
    return pauseIngest(name, pauseIngestSearchIndexOptions());
  }

  /**
   * Pauses updates and maintenance for an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> pauseIngest(final String name, final PauseIngestSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.post(path(pauseIngestPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_PAUSE_INGEST)
      .exec(core)
      .thenApply(response -> null);
  }

  /**
   * Resumes updates and maintenance for an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> resumeIngest(final String name) {
    return resumeIngest(name, resumeIngestSearchIndexOptions());
  }

  /**
   * Resumes updates and maintenance for an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> resumeIngest(final String name, final ResumeIngestSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.post(path(resumeIngestPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_RESUME_INGEST)
      .exec(core)
      .thenApply(response -> null);
  }

  /**
   * Allows querying against an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> allowQuerying(final String name) {
    return allowQuerying(name, allowQueryingSearchIndexOptions());
  }

  /**
   * Allows querying against an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> allowQuerying(final String name, final AllowQueryingSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.post(path(allowQueryingPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_ALLOW_QUERYING)
      .exec(core)
      .thenApply(response -> null);
  }

  /**
   * Disallows querying against an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> disallowQuerying(final String name) {
    return disallowQuerying(name, disallowQueryingSearchIndexOptions());
  }

  /**
   * Disallows querying against an index.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> disallowQuerying(final String name, final DisallowQueryingSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.post(path(disallowQueryingPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_DISALLOW_QUERYING)
      .exec(core)
      .thenApply(response -> null);
  }

  /**
   * Freeze the assignment of index partitions to nodes.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> freezePlan(final String name) {
    return freezePlan(name, freezePlanSearchIndexOptions());
  }

  /**
   * Freeze the assignment of index partitions to nodes.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> freezePlan(final String name, final FreezePlanSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.post(path(freezePlanPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_FREEZE_PLAN)
      .exec(core)
      .thenApply(response -> null);
  }

  /**
   * Unfreeze the assignment of index partitions to nodes.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> unfreezePlan(final String name) {
    return unfreezePlan(name, unfreezePlanSearchIndexOptions());
  }

  /**
   * Unfreeze the assignment of index partitions to nodes.
   *
   * @param name the name of the search index.
   * @return a {@link CompletableFuture} indicating request completion.
   */
  public CompletableFuture<Void> unfreezePlan(final String name, final UnfreezePlanSearchIndexOptions options) {
    notNullOrEmpty(name, "Search Index Name");

    return searchHttpClient.post(path(unfreezePlanPath(name)), options.build())
      .trace(TracingIdentifiers.SPAN_REQUEST_MS_UNFREEZE_PLAN)
      .exec(core)
      .thenApply(response -> null);
  }

}
