package clever.cloud.com.prometheus.metrics

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import fr.davit.akka.http.metrics.core.HttpMetrics._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import fr.davit.akka.http.metrics.prometheus.{Buckets, PrometheusRegistry, PrometheusSettings, Quantiles}
import io.prometheus.client.CollectorRegistry
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}

import scala.util.Failure
import scala.util.Success
import fr.davit.akka.http.metrics.core.scaladsl.HttpMetricsServerBuilder

//#main-class
object QuickstartApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newMeteredServerAt("localhost", 8080, MetricsController.registry).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  /*
  public PrometheusMetricsServlet(MetricRegistry registry) {
 mCollectorRegistry = new CollectorRegistry();
 mCollectorRegistry.register(new DropwizardExports(registry));
}
  */
  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      context.watch(userRegistryActor)

      val mainRoutes = new UserRoutes(userRegistryActor)(context.system)

      val route = mainRoutes.userRoutes ~ MetricsController.routes

      //val routes = MetricsController.routes
      //val routesWithMetrics = metrics(mainRoutes.userRoutes ~ routes).recordMetrics(MetricsController.registry)
      startHttpServer(route)(context.system)
      //val bindingFuture = Http().bindAndHandle(route, "localhost", 9000)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
