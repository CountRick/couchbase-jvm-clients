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

package com.couchbase.client.java.search;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.InvalidArgumentException;
import com.couchbase.client.java.CommonOptions;
import com.couchbase.client.java.codec.JsonSerializer;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.MutationState;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.result.SearchResult;
import com.couchbase.client.java.search.result.SearchRow;
import com.couchbase.client.java.search.sort.SearchSort;

import java.util.HashMap;
import java.util.Map;

import static com.couchbase.client.core.util.Validators.notNullOrEmpty;

public class SearchOptions extends CommonOptions<SearchOptions> {

  private String[] collections;
  private SearchScanConsistency consistency;
  private MutationState consistentWith;
  private boolean disableScoring = false;
  private Boolean explain;
  private Map<String, SearchFacet> facets;
  private String[] fields;
  private String[] highlightFields;
  private HighlightStyle highlightStyle;
  private Integer limit;
  private Map<String, Object> raw;
  private JsonSerializer serializer;
  private Integer skip;
  private JsonArray sort;
  private boolean includeLocations = false;

  public static SearchOptions searchOptions() {
    return new SearchOptions();
  }

  private SearchOptions() {
  }

  /**
   * Allows providing custom JSON key/value pairs for advanced usage.
   * <p>
   * If available, it is recommended to use the methods on this object to customize the search query. This method should
   * only be used if no such setter can be found (i.e. if an undocumented property should be set or you are using
   * an older client and a new server-configuration property has been added to the cluster).
   * <p>
   * Note that the value will be passed through a JSON encoder, so do not provide already encoded JSON as the value. If
   * you want to pass objects or arrays, you can use {@link JsonObject} and {@link JsonArray} respectively.
   *
   * @param key   the parameter name (key of the JSON property)  or empty.
   * @param value the parameter value (value of the JSON property).
   * @return the same {@link QueryOptions} for chaining purposes.
   */
  public SearchOptions raw(final String key, final Object value) {
    notNullOrEmpty(key, "Key");
    if (raw == null) {
      raw = new HashMap<>();
    }
    raw.put(key, value);
    return this;
  }

  /**
   * Add a limit to the query on the number of rows it can return.
   *
   * @param limit the maximum number of rows to return.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions limit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Set the number of rows to skip (eg. for pagination).
   *
   * @param skip the number of results to skip.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions skip(int skip) {
    this.skip = skip;
    return this;
  }

  /**
   * Activates or deactivates the explanation of each result hit in the response, according to the parameter.
   *
   * @param explain should the response include an explanation of each hit (true) or not (false)?
   * @return this SearchQuery for chaining.
   */
  public SearchOptions explain(boolean explain) {
    this.explain = explain;
    return this;
  }

  /**
   * Configures the highlighting of matches in the response.
   * <p>
   * This drives the inclusion of the {@link SearchRow#fragments() fragments} in each {@link SearchRow hit}.
   * <p>
   * Note that to be highlighted, the fields must be stored in the FTS index.
   *
   * @param style  the {@link HighlightStyle} to apply.
   * @param fields the optional fields on which to highlight. If none, all fields where there is a match are highlighted.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions highlight(HighlightStyle style, String... fields) {
    this.highlightStyle = style;
    if (fields != null && fields.length > 0) {
      highlightFields = fields;
    }
    return this;
  }

  /**
   * Configures the highlighting of matches in the response, for the specified fields and using the server's default
   * highlighting style.
   * <p>
   * This drives the inclusion of the {@link SearchRow#fragments() fragments} in each {@link SearchRow hit}.
   * <p>
   * Note that to be highlighted, the fields must be stored in the FTS index.
   *
   * @param fields the optional fields on which to highlight. If none, all fields where there is a match are highlighted.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions highlight(String... fields) {
    return highlight(HighlightStyle.SERVER_DEFAULT, fields);
  }

  /**
   * Configures the highlighting of matches in the response for all fields, using the server's default highlighting
   * style.
   * <p>
   * This drives the inclusion of the {@link SearchRow#fragments() fragments} in each {@link SearchRow hit}.
   * <p>
   * Note that to be highlighted, the fields must be stored in the FTS index.
   *
   * @return this SearchQuery for chaining.
   */
  public SearchOptions highlight() {
    return highlight(HighlightStyle.SERVER_DEFAULT);
  }

  /**
   * Configures the list of fields for which the whole value should be included in the response. If empty, no field
   * values are included.
   * <p>
   * This drives the inclusion of the fields in each {@link SearchRow hit}.
   * <p>
   * Note that to be highlighted, the fields must be stored in the FTS index.
   *
   * @param fields the fields to include.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions fields(String... fields) {
    if (fields != null) {
      this.fields = fields;
    }
    return this;
  }

  /**
   * Allows to limit the search query to a specific list of collection names.
   * <p>
   * NOTE: this is only supported with server 7.0 and later.
   *
   * @param collectionNames the names of the collections this query should be limited to.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions collections(String... collectionNames) {
    if (collectionNames != null) {
      this.collections = collectionNames;
    }
    return this;
  }

  /**
   * Sets the unparameterized consistency to consider for this FTS query. This replaces any
   * consistency tuning previously set.
   *
   * @param consistency the simple consistency to use.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions scanConsistency(SearchScanConsistency consistency) {
    this.consistency = consistency;
    consistentWith = null;
    return this;
  }

  /**
   * Sets mutation tokens this query should be consistent with.
   *
   * @param consistentWith the mutation state to be consistent with.
   * @return this {@link QueryOptions} for chaining.
   */
  public SearchOptions consistentWith(final MutationState consistentWith) {
    this.consistentWith = consistentWith;
    consistency = null;
    return this;
  }

  /**
   * Configures the list of fields (including special fields) which are used for sorting purposes. If empty, the
   * default sorting (descending by score) is used by the server.
   * <p>
   * The list of sort fields can include actual fields (like "firstname" but then they must be stored in the index,
   * configured in the server side mapping). Fields provided first are considered first and in a "tie" case the
   * next sort field is considered. So sorting by "firstname" and then "lastname" will first sort ascending by
   * the firstname and if the names are equal then sort ascending by lastname. Special fields like "_id" and "_score"
   * can also be used. If prefixed with "-" the sort order is set to descending.
   * <p>
   * If no sort is provided, it is equal to sort("-_score"), since the server will sort it by score in descending
   * order.
   *
   * @param sort the fields that should take part in the sorting.
   * @return this SearchQuery for chaining.
   */
  public SearchOptions sort(Object... sort) {
    if (this.sort == null) {
      this.sort = JsonArray.create();
    }

    if (sort != null) {
      for (Object o : sort) {
        if (o instanceof String) {
          this.sort.add((String) o);
        } else if (o instanceof SearchSort) {
          JsonObject params = JsonObject.create();
          ((SearchSort) o).injectParams(params);
          this.sort.add(params);
        } else {
          throw InvalidArgumentException.fromMessage("Only String or SearchSort " +
            "instances are allowed as sort arguments!");
        }
      }
    }
    return this;
  }

  /**
   * Adds one {@link SearchFacet} to the query.
   * <p>
   * This is an additive operation (the given facets are added to any facet previously requested),
   * but if an existing facet has the same name it will be replaced.
   * <p>
   * This drives the inclusion of the facets in the {@link SearchResult}.
   * <p>
   * Note that to be faceted, a field's value must be stored in the FTS index.
   *
   * @param facets the facets to add to the query.
   */
  public SearchOptions facets(final Map<String, SearchFacet> facets) {
    this.facets = facets;
    return this;
  }

  public SearchOptions serializer(final JsonSerializer serializer) {
    this.serializer = serializer;
    return this;
  }

  /**
   * If set to true, thee server will not perform any scoring on the hits.
   *
   * @param disableScoring if scoring should be disabled.
   * @return these {@link SearchOptions} for chaining purposes.
   */
  public SearchOptions disableScoring(final boolean disableScoring) {
    this.disableScoring = disableScoring;
    return this;
  }

  /**
   * If set to true, will include the {@link SearchRow#locations()}.
   *
   * @param includeLocations set to true to inclue the locations.
   * @return these {@link SearchOptions} for chaining purposes.
   */
  public SearchOptions includeLocations(final boolean includeLocations) {
    this.includeLocations = includeLocations;
    return this;
  }

  @Stability.Internal
  public Built build() {
    return new Built();
  }

  public class Built extends BuiltCommonOptions {

    Built() {
    }

    public JsonSerializer serializer() {
      return serializer;
    }

    /**
     * Inject the top level parameters of a query into a prepared {@link JsonObject}
     * that represents the root of the query.
     *
     * @param queryJson the prepared {@link JsonObject} for the whole query.
     */
    @Stability.Internal
    public void injectParams(final String indexName, JsonObject queryJson) {
      if (limit != null && limit >= 0) {
        queryJson.put("size", limit);
      }
      if (skip != null && skip >= 0) {
        queryJson.put("from", skip);
      }
      if (explain != null) {
        queryJson.put("explain", explain);
      }
      if (highlightStyle != null) {
        JsonObject highlight = JsonObject.create();
        if (highlightStyle != HighlightStyle.SERVER_DEFAULT) {
          highlight.put("style", highlightStyle.name().toLowerCase());
        }
        if (highlightFields != null && highlightFields.length > 0) {
          highlight.put("fields", JsonArray.from((Object[]) highlightFields));
        }
        queryJson.put("highlight", highlight);
      }
      if (fields != null && fields.length > 0) {
        queryJson.put("fields", JsonArray.from((Object[]) fields));
      }
      if (sort != null && !sort.isEmpty()) {
        queryJson.put("sort", sort);
      }
      if (disableScoring) {
        queryJson.put("score", "none");
      }

      if (facets != null && !facets.isEmpty()) {
        JsonObject f = JsonObject.create();
        for (Map.Entry<String, SearchFacet> entry : facets.entrySet()) {
          JsonObject facetJson = JsonObject.create();
          entry.getValue().injectParams(facetJson);
          f.put(entry.getKey(), facetJson);
        }
        queryJson.put("facets", f);
      }

      JsonObject control = JsonObject.create();

      if (consistency != null && consistency != SearchScanConsistency.NOT_BOUNDED) {
        JsonObject consistencyJson = JsonObject.create();
        consistencyJson.put("level", consistency.toString());
        control.put("consistency", consistencyJson);
      }

      if (consistentWith != null) {
        JsonObject consistencyJson = JsonObject.create();
        consistencyJson.put("level", "at_plus");
        consistencyJson.put("vectors", JsonObject.create().put(indexName, consistentWith.exportForSearch()));
        control.put("consistency", consistencyJson);
      }

      //if any control was set, inject it
      if (!control.isEmpty()) {
        queryJson.put("ctl", control);
      }

      if (collections != null && collections.length > 0) {
        queryJson.put("collections", JsonArray.from((Object[]) collections));
      }

      if (includeLocations) {
        queryJson.put("includeLocations", true);
      }

      if (raw != null) {
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
          queryJson.put(entry.getKey(), entry.getValue());
        }
      }
    }

  }

}
