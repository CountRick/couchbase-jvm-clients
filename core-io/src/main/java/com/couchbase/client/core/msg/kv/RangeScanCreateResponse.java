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


package com.couchbase.client.core.msg.kv;

import com.couchbase.client.core.kv.CoreRangeScanId;
import com.couchbase.client.core.msg.BaseResponse;
import com.couchbase.client.core.msg.ResponseStatus;

import static com.couchbase.client.core.logging.RedactableArgument.redactMeta;

public class RangeScanCreateResponse extends BaseResponse {

  private final CoreRangeScanId rangeScanId;

  public RangeScanCreateResponse(final ResponseStatus status, final CoreRangeScanId rangeScanId) {
    super(status);
    this.rangeScanId = rangeScanId;
  }

  public CoreRangeScanId rangeScanId() {
    return rangeScanId;
  }

  @Override
  public String toString() {
    return "RangeScanCreateResponse{" +
      "status=" + status() +
      ", rangeScanId=" + redactMeta(rangeScanId) +
      '}';
  }

}
