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

import com.couchbase.client.core.annotation.Stability.Volatile
import com.couchbase.client.core.cnc.RequestSpan
import com.couchbase.client.core.error._
import com.couchbase.client.core.error.context.KeyValueErrorContext
import com.couchbase.client.core.io.CollectionIdentifier
import com.couchbase.client.core.kv.{CoreRangeScanSort, RangeScanOrchestrator}
import com.couchbase.client.core.msg.Response
import com.couchbase.client.core.msg.kv._
import com.couchbase.client.core.retry.RetryStrategy
import com.couchbase.client.core.service.kv.{Observe, ObserveContext}
import com.couchbase.client.core.{Core, CoreKeyspace}
import com.couchbase.client.scala.codec._
import com.couchbase.client.scala.durability.Durability._
import com.couchbase.client.scala.durability._
import com.couchbase.client.scala.env.ClusterEnvironment
import com.couchbase.client.scala.kv._
import com.couchbase.client.scala.kv.handlers._
import com.couchbase.client.scala.util.CoreCommonConverters.{convert, convertExpiry, encoder, makeCommonOptions}
import com.couchbase.client.scala.util.{ExpiryUtil, FutureConversions, TimeoutUtil}
import reactor.core.scala.publisher.{SFlux, SMono}

import java.time.Instant
import java.util
import java.util.Optional
import scala.compat.java8.FutureConverters
import scala.compat.java8.OptionConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

private[scala] case class HandlerParams(
    core: Core,
    bucketName: String,
    collectionIdentifier: CollectionIdentifier,
    env: ClusterEnvironment
) {
  def tracer = env.coreEnv.requestTracer()
}

private[scala] case class HandlerBasicParams(core: Core, env: ClusterEnvironment) {
  def tracer = env.coreEnv.requestTracer()
}

/** Provides asynchronous access to all collection APIs, based around Scala `Future`s.  This is the main entry-point
  * for key-value (KV) operations.
  *
  * <p>If synchronous, blocking access is needed, we recommend looking at the [[Collection]].  If a more advanced
  * async API based around reactive programming is desired, then check out the [[ReactiveCollection]].
  *
  * @author Graham Pople
  * @since 1.0.0
  * @define Same             This asynchronous version performs the same functionality and takes the same parameters,
  *                          but returns the same result object asynchronously in a `Future`.
  * */
class AsyncCollection(
    val name: String,
    val bucketName: String,
    val scopeName: String,
    val core: Core,
    val environment: ClusterEnvironment
) {
  private[scala] implicit val ec: ExecutionContext = environment.ec

  import com.couchbase.client.scala.util.DurationConversions._

  private[scala] val kvTimeout: Durability => Duration = TimeoutUtil.kvTimeout(environment)
  private[scala] val kvReadTimeout: Duration           = environment.timeoutConfig.kvTimeout()
  private[scala] val collectionIdentifier =
    new CollectionIdentifier(bucketName, Optional.of(scopeName), Optional.of(name))
  private[scala] val hp                    = HandlerParams(core, bucketName, collectionIdentifier, environment)
  private[scala] val getSubDocHandler      = new GetSubDocumentHandler(hp)
  private[scala] val mutateInHandler       = new MutateInHandler(hp)
  private[scala] val getFromReplicaHandler = new GetFromReplicaHandler(hp)
  private[scala] val rangeScanOrchestrator = new RangeScanOrchestrator(core, collectionIdentifier)
  private[scala] val kvOps                 = core.kvOps(CoreKeyspace.from(collectionIdentifier))

  val binary = new AsyncBinaryCollection(this)

  private[scala] def wrap[Resp <: Response, Res](
      in: Try[KeyValueRequest[Resp]],
      id: String,
      handler: KeyValueRequestHandler[Resp, Res]
  ): Future[Res] = {
    AsyncCollection.wrap(in, id, handler, core)
  }

  private[scala] def wrapWithDurability[Resp <: Response, Res <: HasDurabilityTokens](
      in: Try[KeyValueRequest[Resp]],
      id: String,
      handler: KeyValueRequestHandler[Resp, Res],
      durability: Durability,
      remove: Boolean,
      timeout: java.time.Duration
  ): Future[Res] = {
    AsyncCollection.wrapWithDurability(
      in,
      id,
      handler,
      durability,
      remove,
      timeout,
      core,
      bucketName,
      collectionIdentifier
    )
  }

  /** Inserts a full document into this collection, if it does not exist already.
    *
    * $Same */
  def insert[T](
      id: String,
      content: T,
      durability: Durability = Disabled,
      timeout: Duration = Duration.MinusInf
  )(implicit serializer: JsonSerializer[T]): Future[MutationResult] = {
    convert(kvOps.insertAsync(makeCommonOptions(timeout),
      id,
      encoder(environment.transcoder, serializer, content),
      convert(durability),
      0)).map(result => convert(result))
  }

  /** Inserts a full document into this collection, if it does not exist already.
    *
    * $Same */
  def insert[T](
      id: String,
      content: T,
      options: InsertOptions
  )(implicit serializer: JsonSerializer[T]): Future[MutationResult] = {
    convert(kvOps.insertAsync(convert(options),
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
  )(implicit serializer: JsonSerializer[T]): Future[MutationResult] = {
    convert(kvOps.replaceAsync(makeCommonOptions(timeout),
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
  )(implicit serializer: JsonSerializer[T]): Future[MutationResult] = {
    convert(kvOps.replaceAsync(convert(options),
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
  )(implicit serializer: JsonSerializer[T]): Future[MutationResult] = {
    convert(kvOps.upsertAsync(makeCommonOptions(timeout),
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
  )(implicit serializer: JsonSerializer[T]): Future[MutationResult] = {
    convert(kvOps.upsertAsync(convert(options),
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
  ): Future[MutationResult] = {
    convert(kvOps.removeAsync(makeCommonOptions(timeout),
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
  ): Future[MutationResult] = {
    convert(kvOps.removeAsync(convert(options),
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
  ): Future[GetResult] = {
    convert(kvOps.getAsync(makeCommonOptions(timeout),
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
  ): Future[GetResult] = {
    convert(kvOps.getAsync(convert(options),
      id,
      options.project.asJava,
      options.withExpiry))
            .map(result => convert(result, environment, options.transcoder))
  }

  private def getSubDoc(
      id: String,
      spec: collection.Seq[LookupInSpec],
      withExpiry: Boolean,
      timeout: Duration,
      retryStrategy: RetryStrategy,
      transcoder: Transcoder,
      parentSpan: Option[RequestSpan]
  ): Future[LookupInResult] = {
    val req = getSubDocHandler.request(id, spec, withExpiry, timeout, retryStrategy, parentSpan)
    req match {
      case Success(request) =>
        core.send(request)

        val out = FutureConverters
          .toScala(request.response())
          .map(response => {
            getSubDocHandler.response(request, id, response, withExpiry, transcoder)
          })

        out onComplete {
          case Success(_)                              => request.context.logicallyComplete()
          case Failure(err: DocumentNotFoundException) => request.context.logicallyComplete()
          case Failure(err)                            => request.context.logicallyComplete(err)
        }

        out

      case Failure(err) => Future.failed(err)
    }
  }

  /** Sub-Document mutations allow modifying parts of a JSON document directly, which can be more efficiently than
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
  ): Future[MutateInResult] = {
    val opts = MutateInOptions().cas(cas).document(document).durability(durability).timeout(timeout)
    mutateIn(id, spec, opts)
  }

  /** Sub-Document mutations allow modifying parts of a JSON document directly, which can be more efficiently than
    * fetching and modifying the full document.
    *
    * $Same */
  def mutateIn(
      id: String,
      spec: collection.Seq[MutateInSpec],
      options: MutateInOptions
  ): Future[MutateInResult] = {

    val timeoutActual =
      if (options.timeout == Duration.MinusInf) kvTimeout(options.durability) else options.timeout

    val req: SMono[SubdocMutateRequest] = mutateInHandler.request(
      id,
      spec,
      options.cas,
      options.document,
      options.durability,
      ExpiryUtil.expiryActual(options.expiry, options.expiryTime),
      options.preserveExpiry,
      timeoutActual,
      options.retryStrategy.getOrElse(environment.retryStrategy),
      options.accessDeleted,
      options.createAsDeleted,
      options.transcoder.getOrElse(environment.transcoder),
      options.parentSpan
    )

    req.toFuture.flatMap(request => {
      core.send(request)

      val out = FutureConverters
        .toScala(request.response())
        .map(response => mutateInHandler.response(request, id, options.document, response))

      out onComplete {
        case Success(_)   => request.context.logicallyComplete()
        case Failure(err) => request.context.logicallyComplete(err)
      }

      options.durability match {
        case ClientVerified(replicateTo, persistTo) =>
          out.flatMap(response => {

            val observeCtx = new ObserveContext(
              core.context(),
              PersistTo.asCore(persistTo),
              ReplicateTo.asCore(replicateTo),
              response.mutationToken.asJava,
              response.cas,
              collectionIdentifier,
              id,
              false,
              timeoutActual,
              request.requestSpan()
            )

            FutureConversions
              .javaMonoToScalaFuture(Observe.poll(observeCtx))
              // After the observe return the original response
              .map(_ => response)
          })

        case _ => out
      }
    })
  }

  /** Fetches a full document from this collection, and simultaneously lock the document from writes.
    *
    * $Same */
  def getAndLock(
      id: String,
      lockTime: Duration,
      timeout: Duration = kvReadTimeout
  ): Future[GetResult] = {
    convert(kvOps.getAndLockAsync(makeCommonOptions(timeout),
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
  ): Future[GetResult] = {
    convert(kvOps.getAndLockAsync(convert(options),
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
  ): Future[Unit] = {
    convert(kvOps.unlockAsync(makeCommonOptions(timeout),
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
  ): Future[Unit] = {
    convert(kvOps.unlockAsync(convert(options),
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
  ): Future[GetResult] = {
    convert(kvOps.getAndTouchAsync(makeCommonOptions(timeout),
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
  ): Future[GetResult] = {
    convert(kvOps.getAndTouchAsync(convert(options),
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
  ): Future[LookupInResult] = {
    val opts = LookupInOptions().timeout(timeout)
    lookupIn(id, spec, opts)
  }

  /** SubDocument lookups allow retrieving parts of a JSON document directly, which may be more efficient than
    * retrieving the entire document.
    *
    * $Same */
  def lookupIn(
      id: String,
      spec: collection.Seq[LookupInSpec],
      options: LookupInOptions
  ): Future[LookupInResult] = {
    val timeout = if (options.timeout == Duration.MinusInf) kvReadTimeout else options.timeout
    getSubDoc(
      id,
      spec,
      options.withExpiry,
      timeout,
      options.retryStrategy.getOrElse(environment.retryStrategy),
      options.transcoder.getOrElse(environment.transcoder),
      options.parentSpan
    )
  }

  /** Retrieves any available version of the document.
    *
    * $Same */
  def getAnyReplica(
      id: String,
      timeout: Duration = kvReadTimeout
  ): Future[GetReplicaResult] = {
    val opts = GetAnyReplicaOptions().timeout(timeout)
    getAnyReplica(id, opts)
  }

  /** Retrieves any available version of the document.
    *
    * $Same */
  def getAnyReplica(
      id: String,
      options: GetAnyReplicaOptions
  ): Future[GetReplicaResult] = {
    getAllReplicas(id, options.convert).take(1).head
  }

  /** Retrieves all available versions of the document.
    *
    * $Same */
  def getAllReplicas(
      id: String,
      timeout: Duration = kvReadTimeout
  ): Seq[Future[GetReplicaResult]] = {
    val opts = GetAllReplicasOptions().timeout(timeout)
    getAllReplicas(id, opts)
  }

  /** Retrieves all available versions of the document.
    *
    * $Same */
  def getAllReplicas(
      id: String,
      options: GetAllReplicasOptions
  ): Seq[Future[GetReplicaResult]] = {
    val timeout = if (options.timeout == Duration.MinusInf) kvReadTimeout else options.timeout
    val reqsTry: Try[Seq[GetRequest]] =
      getFromReplicaHandler.requestAll(
        id,
        timeout,
        options.retryStrategy.getOrElse(environment.retryStrategy),
        options.parentSpan
      )

    reqsTry match {
      case Failure(err) => Seq(Future.failed(err))

      case Success(reqs: Seq[GetRequest]) =>
        reqs.map(request => {
          core.send(request)

          val out = FutureConverters
            .toScala(request.response())
            .flatMap(response => {
              val isReplica = request match {
                case _: GetRequest => false
                case _             => true
              }
              getFromReplicaHandler.response(
                request,
                id,
                response,
                isReplica,
                options.transcoder.getOrElse(environment.transcoder)
              ) match {
                case Some(x) => Future.successful(x)
                case _ =>
                  val ctx = KeyValueErrorContext.completedRequest(request, response)
                  Future.failed(new DocumentNotFoundException(ctx))
              }
            })

          out onComplete {
            case Success(_)                              => request.context.logicallyComplete()
            case Failure(err: DocumentNotFoundException) => request.context.logicallyComplete()
            case Failure(err)                            => request.context.logicallyComplete(err)
          }
          out
        })
    }
  }

  /** Checks if a document exists.
    *
    * $Same */
  def exists(
      id: String,
      timeout: Duration = kvReadTimeout
  ): Future[ExistsResult] = {
    convert(kvOps.existsAsync(makeCommonOptions(timeout), id))
            .map(result => convert(result))
  }

  /** Checks if a document exists.
    *
    * $Same */
  def exists(
      id: String,
      options: ExistsOptions
  ): Future[ExistsResult] = {
    convert(kvOps.existsAsync(convert(options), id))
            .map(result => convert(result))
  }

  /** Updates the expiry of the document with the given id.
    *
    * $Same */
  def touch(
      id: String,
      expiry: Duration,
      timeout: Duration = kvReadTimeout
  ): Future[MutationResult] = {
    convert(kvOps.touchAsync(makeCommonOptions(timeout),
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
  ): Future[MutationResult] = {
    convert(kvOps.touchAsync(convert(options),
      id,
      ExpiryUtil.expiryActual(expiry, None)))
            .map(result => convert(result))
  }

  private[scala] def scanRequest(scanType: ScanType, opts: ScanOptions): SFlux[ScanResult] = {
    import scala.compat.java8.OptionConverters._

    val timeoutActual: java.time.Duration =
      if (opts.timeout == Duration.MinusInf) environment.timeoutConfig.kvScanTimeout()
      else opts.timeout

    val sortCore = opts.scanSort match {
      case Some(ScanSort.Ascending) => CoreRangeScanSort.ASCENDING
      case _                        => CoreRangeScanSort.NONE
    }

    val consistencyTokens = new java.util.HashMap[java.lang.Short, MutationToken]()

    opts.consistentWith match {
      case Some(cw) => cw.tokens.foreach(t => consistencyTokens.put(t.partitionID, t))
      case _        =>
    }

    val idsOnly = opts.idsOnly.getOrElse(false)

    val flux = scanType match {
      case scan: ScanType.RangeScan =>
        FutureConversions.javaFluxToScalaFlux(
          rangeScanOrchestrator.rangeScan(
            scan.from.term,
            scan.from.exclusive,
            scan.to.term,
            scan.to.exclusive,
            timeoutActual,
            opts.batchItemLimit
              .getOrElse(RangeScanOrchestrator.RANGE_SCAN_DEFAULT_BATCH_ITEM_LIMIT),
            opts.batchByteLimit
              .getOrElse(RangeScanOrchestrator.RANGE_SCAN_DEFAULT_BATCH_BYTE_LIMIT),
            opts.idsOnly.getOrElse(false),
            sortCore,
            opts.parentSpan.asJava,
            consistencyTokens
          )
        )
      case scan: ScanType.SamplingScan =>
        if (scan.limit <= 0) {
          SFlux.error(new InvalidArgumentException("Limit must be > 0", null, null))
        } else {
          FutureConversions.javaFluxToScalaFlux(
            rangeScanOrchestrator.samplingScan(
              scan.limit,
              Optional.of(scan.seed),
              timeoutActual,
              opts.batchItemLimit.getOrElse(0),
              opts.batchByteLimit.getOrElse(0),
              opts.idsOnly.getOrElse(false),
              sortCore,
              opts.parentSpan.asJava,
              consistencyTokens
            )
          )
        }
    }

    flux.map(
      item =>
        if (idsOnly) {
          ScanResult(
            item.key(),
            idOnly = true,
            None,
            item.flags(),
            None,
            None,
            opts.transcoder.getOrElse(environment.transcoder)
          )
        } else {
          ScanResult(
            item.key(),
            idOnly = false,
            Some(item.value()),
            item.flags(),
            Some(item.cas()),
            Option(item.expiry()),
            opts.transcoder.getOrElse(environment.transcoder)
          )
        }
    )
  }

  /** Initiates a KV range scan, which will return a non-blocking stream of KV documents.
    *
    * Uses default options.
    */
  @Volatile
  def scan(scanType: ScanType): Future[Iterator[ScanResult]] = {
    scan(scanType, ScanOptions())
  }

  /** Initiates a KV range scan, which will return a non-blocking stream of KV documents.
    */
  @Volatile
  def scan(scanType: ScanType, opts: ScanOptions): Future[Iterator[ScanResult]] = {
    Future(scanRequest(scanType, opts).toStream().iterator)
  }
}

object AsyncCollection {
  private[scala] val EmptyList = new java.util.ArrayList[String]()

  private def wrap[Resp <: Response, Res](
      in: Try[KeyValueRequest[Resp]],
      id: String,
      handler: KeyValueRequestHandler[Resp, Res],
      core: Core
  )(implicit ec: ExecutionContext): Future[Res] = {
    in match {
      case Success(request) =>
        core.send[Resp](request)

        val out = FutureConverters
          .toScala(request.response())
          .map(response => handler.response(request, id, response))

        out onComplete {
          case Success(_)   => request.context.logicallyComplete()
          case Failure(err) => request.context.logicallyComplete(err)
        }

        out

      case Failure(err) => Future.failed(err)
    }
  }

  private def wrapWithDurability[Resp <: Response, Res <: HasDurabilityTokens](
      in: Try[KeyValueRequest[Resp]],
      id: String,
      handler: KeyValueRequestHandler[Resp, Res],
      durability: Durability,
      remove: Boolean,
      timeout: java.time.Duration,
      core: Core,
      bucketName: String,
      collectionidentifier: CollectionIdentifier
  )(implicit ec: ExecutionContext): Future[Res] = {
    val initial: Future[Res] = wrap(in, id, handler, core)

    durability match {
      case ClientVerified(replicateTo, persistTo) =>
        initial.flatMap(response => {

          val observeCtx = new ObserveContext(
            core.context(),
            PersistTo.asCore(persistTo),
            ReplicateTo.asCore(replicateTo),
            response.mutationToken.asJava,
            response.cas,
            collectionidentifier,
            id,
            remove,
            timeout,
            in.get.requestSpan()
          )

          FutureConversions
            .javaMonoToScalaFuture(Observe.poll(observeCtx))
            // After the observe return the original response
            .map(_ => response)
        })

      case _ => initial
    }
  }
}
