/*
 * Copyright (c) 2019 Couchbase, Inc.
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

package com.couchbase.client.scala

import com.couchbase.client.core.CoreKeyspace
import com.couchbase.client.core.annotation.Stability.Volatile
import com.couchbase.client.core.msg.kv.GetRequest
import com.couchbase.client.scala.codec.JsonSerializer
import com.couchbase.client.scala.durability.Durability
import com.couchbase.client.scala.durability.Durability._
import com.couchbase.client.scala.kv._
import com.couchbase.client.scala.util.CoreCommonConverters.{convert, convertExpiry, encoder, makeCommonOptions}
import com.couchbase.client.scala.util.{ExpiryUtil, FutureConversions, TimeoutUtil}
import reactor.core.scala.publisher.{SFlux, SMono}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/** Provides asynchronous access to all collection APIs, based around reactive programming using the
  * [[https://projectreactor.io/ Project Reactor]] library.  This is the main entry-point
  * for key-value (KV) operations.
  *
  * <p>If synchronous, blocking access is needed, we recommend looking at the [[Collection]].  If a simpler
  * async API based around Scala `Future`s is desired, then check out the [[AsyncCollection]].
  *
  * @author Graham Pople
  * @since 1.0.0
  * @define Same             This reactive programming version performs the same functionality and takes the same
  *                          parameters, but returns the same result object asynchronously in a Project Reactor `SMono`.
  *                          See the documentation for the matching method in [[Collection]].
  * */
class ReactiveCollection(async: AsyncCollection) {
  private[scala] val kvTimeout: Durability => Duration = TimeoutUtil.kvTimeout(async.environment)
  private[scala] val kvReadTimeout: Duration           = async.kvReadTimeout
  private val environment                              = async.environment
  private val core                                     = async.core
  private implicit val ec: ExecutionContext            = async.ec
  private[scala] val kvOps                             = async.kvOps

  import com.couchbase.client.scala.util.DurationConversions._

  def name: String       = async.name
  def bucketName: String = async.bucketName
  def scopeName: String  = async.scopeName

  /** Inserts a full document into this collection, if it does not exist already.
    *
    * $Same */
  def insert[T](
      id: String,
      content: T,
      durability: Durability = Disabled,
      timeout: Duration = Duration.MinusInf
  )(implicit serializer: JsonSerializer[T]): SMono[MutationResult] = {
    convert(kvOps.insertReactive(makeCommonOptions(timeout),
      id,
      encoder(environment.transcoder, serializer, content),
      convert(durability),
      0))
            .map(result => convert(result))
  }

  /** Inserts a full document into this collection, if it does not exist already.
    *
    * $Same */
  def insert[T](
      id: String,
      content: T,
      options: InsertOptions
  )(implicit serializer: JsonSerializer[T]): SMono[MutationResult] = {
    convert(kvOps.insertReactive(convert(options),
      id,
      encoder(options.transcoder.getOrElse(environment.transcoder), serializer, content),
      convert(options.durability),
      ExpiryUtil.expiryActual(options.expiry, options.expiryTime)))
            .map(result => convert(result))
  }

  /** Replaces the contents of a full document in this collection, if it already exists.
    *
    * $Same */
  def replace[T](
      id: String,
      content: T,
      cas: Long = 0,
      durability: Durability = Disabled,
      timeout: Duration = Duration.MinusInf
  )(implicit serializer: JsonSerializer[T]): SMono[MutationResult] = {
    convert(kvOps.replaceReactive(makeCommonOptions(timeout),
      id,
      encoder(environment.transcoder, serializer, content),
      cas,
      convert(durability),
      0,
      false))
            .map(result => convert(result))
  }

  /** Replaces the contents of a full document in this collection, if it already exists.
    *
    * $Same */
  def replace[T](
      id: String,
      content: T,
      options: ReplaceOptions
  )(implicit serializer: JsonSerializer[T]): SMono[MutationResult] = {
    convert(kvOps.replaceReactive(convert(options),
      id,
      encoder(options.transcoder.getOrElse(environment.transcoder), serializer, content),
      options.cas,
      convert(options.durability),
      ExpiryUtil.expiryActual(options.expiry, options.expiryTime),
      options.preserveExpiry))
            .map(result => convert(result))
  }

  /** Upserts the contents of a full document in this collection.
    *
    * $Same */
  def upsert[T](
      id: String,
      content: T,
      durability: Durability = Disabled,
      timeout: Duration = Duration.MinusInf
  )(implicit serializer: JsonSerializer[T]): SMono[MutationResult] = {
    convert(kvOps.upsertReactive(makeCommonOptions(timeout),
      id,
      encoder(environment.transcoder, serializer, content),
      convert(durability),
      0,
      false))
            .map(result => convert(result))
  }

  /** Upserts the contents of a full document in this collection.
    *
    * $Same */
  def upsert[T](
      id: String,
      content: T,
      options: UpsertOptions
  )(implicit serializer: JsonSerializer[T]): SMono[MutationResult] = {
    convert(kvOps.upsertReactive(convert(options),
      id,
      encoder(options.transcoder.getOrElse(environment.transcoder), serializer, content),
      convert(options.durability),
      ExpiryUtil.expiryActual(options.expiry, options.expiryTime),
      options.preserveExpiry))
            .map(result => convert(result))
  }

  /** Removes a document from this collection, if it exists.
    *
    * $Same */
  def remove(
      id: String,
      cas: Long = 0,
      durability: Durability = Disabled,
      timeout: Duration = Duration.MinusInf
  ): SMono[MutationResult] = {
    convert(kvOps.removeReactive(makeCommonOptions(timeout),
      id,
      cas,
      convert(durability)))
            .map(result => convert(result))
  }

  /** Removes a document from this collection, if it exists.
    *
    * $Same */
  def remove(
      id: String,
      options: RemoveOptions
  ): SMono[MutationResult] = {
    convert(kvOps.removeReactive(convert(options),
      id,
      options.cas,
      convert(options.durability)))
            .map(result => convert(result))
  }

  /** Fetches a full document from this collection.
    *
    * $Same */
  def get(
      id: String,
      timeout: Duration = kvReadTimeout
  ): SMono[GetResult] = {
    convert(kvOps.getReactive(makeCommonOptions(timeout),
      id,
      AsyncCollection.EmptyList,
      false))
            .map(result => convert(result, environment, None))
  }

  /** Fetches a full document from this collection.
    *
    * $Same */
  def get(
      id: String,
      options: GetOptions
  ): SMono[GetResult] = {
    convert(kvOps.getReactive(convert(options),
      id,
      options.project.asJava,
      options.withExpiry))
            .map(result => convert(result, environment, options.transcoder))
  }

  /** SubDocument mutations allow modifying parts of a JSON document directly, which can be more efficiently than
    * fetching and modifying the full document.
    *
    * $Same */
  def mutateIn(
      id: String,
      spec: collection.Seq[MutateInSpec],
      cas: Long = 0,
      document: StoreSemantics = StoreSemantics.Replace,
      durability: Durability = Disabled,
      timeout: Duration = Duration.MinusInf
  ): SMono[MutateInResult] = {
    SMono.defer(
      () => SMono.fromFuture(async.mutateIn(id, spec, cas, document, durability, timeout))
    )
  }

  /** SubDocument mutations allow modifying parts of a JSON document directly, which can be more efficiently than
    * fetching and modifying the full document.
    *
    * $Same */
  def mutateIn(
      id: String,
      spec: collection.Seq[MutateInSpec],
      options: MutateInOptions
  ): SMono[MutateInResult] = {
    SMono.defer(() => SMono.fromFuture(async.mutateIn(id, spec, options)))
  }

  /** Fetches a full document from this collection, and simultaneously lock the document from writes.
    *
    * $Same */
  def getAndLock(
      id: String,
      lockTime: Duration,
      timeout: Duration = kvReadTimeout
  ): SMono[GetResult] = {
    convert(kvOps.getAndLockReactive(makeCommonOptions(timeout),
      id,
      convert(lockTime)))
            .map(result => convert(result, environment, None))
  }

  /** Fetches a full document from this collection, and simultaneously lock the document from writes.
    *
    * $Same */
  def getAndLock(
      id: String,
      lockTime: Duration,
      options: GetAndLockOptions
  ): SMono[GetResult] = {
    convert(kvOps.getAndLockReactive(convert(options),
      id,
      convert(lockTime)))
            .map(result => convert(result, environment, options.transcoder))
  }

  /** Unlock a locked document.
    *
    * $Same */
  def unlock(
      id: String,
      cas: Long,
      timeout: Duration = kvReadTimeout
  ): SMono[Unit] = {
    convert(kvOps.unlockReactive(makeCommonOptions(timeout),
      id,
      cas)).map(_ => ())
  }

  /** Unlock a locked document.
    *
    * $Same */
  def unlock(
      id: String,
      cas: Long,
      options: UnlockOptions
  ): SMono[Unit] = {
    convert(kvOps.unlockReactive(convert(options),
      id,
      cas)).map(_ => ())
  }

  /** Fetches a full document from this collection, and simultaneously update the expiry value of the document.
    *
    * $Same */
  def getAndTouch(
      id: String,
      expiry: Duration,
      timeout: Duration = kvReadTimeout
  ): SMono[GetResult] = {
    convert(kvOps.getAndTouchReactive(makeCommonOptions(timeout),
      id,
      convertExpiry(expiry)))
            .map(result => convert(result, environment, None))
  }

  /** Fetches a full document from this collection, and simultaneously update the expiry value of the document.
    *
    * $Same */
  def getAndTouch(
      id: String,
      expiry: Duration,
      options: GetAndTouchOptions
  ): SMono[GetResult] = {
    convert(kvOps.getAndTouchReactive(convert(options),
      id,
      convertExpiry(expiry)))
            .map(result => convert(result, environment, options.transcoder))
  }

  /** SubDocument lookups allow retrieving parts of a JSON document directly, which may be more efficient than
    * retrieving the entire document.
    *
    * $Same */
  def lookupIn(
      id: String,
      spec: collection.Seq[LookupInSpec],
      timeout: Duration = kvReadTimeout
  ): SMono[LookupInResult] = {
    SMono.defer(() => SMono.fromFuture(async.lookupIn(id, spec, timeout)))
  }

  /** SubDocument lookups allow retrieving parts of a JSON document directly, which may be more efficient than
    * retrieving the entire document.
    *
    * $Same */
  def lookupIn(
      id: String,
      spec: collection.Seq[LookupInSpec],
      options: LookupInOptions
  ): SMono[LookupInResult] = {
    SMono.defer(() => SMono.fromFuture(async.lookupIn(id, spec, options)))
  }

  /** Retrieves any available version of the document.
    *
    * $Same */
  def getAnyReplica(
      id: String,
      timeout: Duration = kvReadTimeout
  ): SMono[GetReplicaResult] = {
    SMono.defer(() => getAnyReplica(id, GetAnyReplicaOptions().timeout(timeout)))
  }

  /** Retrieves any available version of the document.
    *
    * $Same */
  def getAnyReplica(
      id: String,
      options: GetAnyReplicaOptions
  ): SMono[GetReplicaResult] = {
    val timeout = if (options.timeout == Duration.MinusInf) kvReadTimeout else options.timeout
    getAllReplicas(id, options.convert)
      .timeout(timeout)
      .next()
  }

  /** Retrieves all available versions of the document.
    *
    * $Same */
  def getAllReplicas(
      id: String,
      timeout: Duration = kvReadTimeout
  ): SFlux[GetReplicaResult] = {
    getAllReplicas(id, GetAllReplicasOptions().timeout(timeout))
  }

  /** Retrieves all available versions of the document.
    *
    * $Same */
  def getAllReplicas(
      id: String,
      options: GetAllReplicasOptions
  ): SFlux[GetReplicaResult] = {
    val retryStrategy = options.retryStrategy.getOrElse(environment.retryStrategy)
    val transcoder    = options.transcoder.getOrElse(environment.transcoder)
    val timeout       = if (options.timeout == Duration.MinusInf) kvReadTimeout else options.timeout

    val reqsTry: Try[Seq[GetRequest]] =
      async.getFromReplicaHandler.requestAll(id, timeout, retryStrategy, options.parentSpan)

    reqsTry match {
      case Failure(err) => SFlux.error(err)

      case Success(reqs: Seq[GetRequest]) =>
        SFlux.defer({
          val monos: Seq[SMono[GetReplicaResult]] = reqs.map(request => {
            core.send(request)

            FutureConversions
              .javaCFToScalaMono(request, request.response(), propagateCancellation = true)
              .flatMap(r => {
                val isReplica = request match {
                  case _: GetRequest => false
                  case _             => true
                }
                async.getFromReplicaHandler.response(request, id, r, isReplica, transcoder) match {
                  case Some(getResult) => SMono.just(getResult)
                  case _               => SMono.empty[GetReplicaResult]
                }
              })
              .doOnNext(_ => request.context.logicallyComplete)
              .doOnError(err => request.context().logicallyComplete(err))
          })

          SFlux.mergeSequential(monos).timeout(timeout)
        })
    }

  }

  /** Updates the expiry of the document with the given id.
    *
    * $Same */
  def touch(
      id: String,
      expiry: Duration,
      timeout: Duration = kvReadTimeout
  ): SMono[MutationResult] = {
    convert(kvOps.touchReactive(makeCommonOptions(timeout),
      id,
      convertExpiry(expiry)))
            .map(result => convert(result))
  }

  /** Updates the expiry of the document with the given id.
    *
    * $Same */
  def touch(
      id: String,
      expiry: Duration,
      options: TouchOptions
  ): SMono[MutationResult] = {
    convert(kvOps.touchReactive(convert(options),
      id,
      convertExpiry(expiry)))
            .map(result => convert(result))
  }

  /** Checks if a document exists.
    *
    * $Same */
  def exists(
      id: String,
      timeout: Duration = kvReadTimeout
  ): SMono[ExistsResult] = {
    convert(kvOps.existsReactive(makeCommonOptions(timeout), id))
            .map(result => convert(result))
  }

  /** Checks if a document exists.
    *
    * $Same */
  def exists(
      id: String,
      options: ExistsOptions
  ): SMono[ExistsResult] = {
    convert(kvOps.existsReactive(convert(options), id))
            .map(result => convert(result))
  }

  /** Initiates a KV range scan, which will return a stream of KV documents.
    *
    * Uses default options.
    */
  @Volatile
  def scan(scanType: ScanType): SFlux[ScanResult] = {
    scan(scanType, ScanOptions())
  }

  /** Initiates a KV range scan, which will return a stream of KV documents.
    */
  @Volatile
  def scan(scanType: ScanType, opts: ScanOptions): SFlux[ScanResult] = {
    async.scanRequest(scanType, opts)
  }
}
