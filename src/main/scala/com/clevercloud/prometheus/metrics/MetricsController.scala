package com.clevercloud.prometheus.metrics

import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import fr.davit.akka.http.metrics.prometheus.{Buckets, PrometheusRegistry, PrometheusSettings, Quantiles}
import io.prometheus.client.CollectorRegistry

class MetricsController {
  import MetricsController._

  val route: Route = (get & path("metrics")) (metrics(registry))
}

object MetricsController {
  val routes: Route = new MetricsController().route

  private val settings: PrometheusSettings = PrometheusSettings
    .default
    .withIncludePathDimension(true)
    .withIncludeMethodDimension(true)
    .withIncludeStatusDimension(true)
    .withDurationConfig(Buckets(5.0))
    .withReceivedBytesConfig(Quantiles())
    .withSentBytesConfig(Buckets(100.0))
    .withDefineError(_.status.isFailure)

  private val collector: CollectorRegistry = CollectorRegistry.defaultRegistry

  val registry: PrometheusRegistry = PrometheusRegistry(collector, settings)
}