package com.clevercloud.prometheus.metrics

//#user-registry-actor
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Scheduler => TypedScheduler}
import io.prometheus.client.Counter
import io.prometheus.client.CollectorRegistry

//#user-case-classes

// RegisterMetrics used to declare a custom metrics, a single object there is no labels for this Metrics, a case class with parameter specifies the labels to set for a Metrics 
sealed trait RegisterMetrics
case object RegisterUserCounter extends RegisterMetrics
case class RegisterRouteCounter(route: String, verb: String) extends RegisterMetrics

// MetricsAction the action done by the MetricsActor, in case of a Counter only increment action
sealed trait MetricsAction
case class IncrementCounter(counter: RegisterMetrics) extends MetricsAction
case class IncrementCounterBy(counter: RegisterMetrics, by: Long) extends MetricsAction

// MetricsProperties the properties as the name or the help field of a RegisterMetrics
object MetricsProperties {
  def apply(counter: RegisterMetrics): MetricsProperties = {
    counter match {
      case RegisterUserCounter => MetricsProperties("my_awesome_counter_user_from_actor", "Help example: Total user requests.")
      case RegisterRouteCounter(_,_) => MetricsProperties("my_awesome_counter_route_from_actor", "Help example: Total route requests.")
    }
  }
}
case class MetricsProperties(name: String, help: String)

//#user-case-classe
object MetricsActor {
  def apply(
    // Expect a valid Prometheus registry as input to register the metrics to collect
    registry: CollectorRegistry
  ): Behavior[MetricsAction] =
    Behaviors.setup(context => new MetricsActor(registry, context))
}

// MetricsActor register and applies action on custom Prometheus Metrics 
class MetricsActor(registry: CollectorRegistry, context: ActorContext[MetricsAction]) extends AbstractBehavior[MetricsAction](context) {
  // actor protocol
  implicit val scheduler: TypedScheduler = schedulerFromActorSystem(context.system)
  implicit val system = context.system
  implicit val executionContext = system.executionContext

  // registeredCounter known registerd metrics
  var registeredCounter = scala.collection.mutable.Map[String, Counter]()

  // onMessage Actions to perform when the Metrics actor receive a message
  override def onMessage(message: MetricsAction): Behavior[MetricsAction] = message match {
      case IncrementCounter(counter) => {
          val prometheusCounter = registeredCounter.getOrElse(MetricsProperties(counter).name, registerUserCounter(counter))
          incrementUserCounter(counter, prometheusCounter, None)
          Behaviors.same
      }
      case IncrementCounterBy(counter, by) => {
          val prometheusCounter = registeredCounter.getOrElse(MetricsProperties(counter).name, registerUserCounter(counter))
          incrementUserCounter(counter, prometheusCounter, Some(by))
          Behaviors.same
      }
    }

  // registerUserCounter will register a user Metrics counter
  def registerUserCounter(counter: RegisterMetrics): Counter = {
    val counterProperties = MetricsProperties(counter)

    // Load labelNames, will be empty for an Object class
    val labelNames = counter.getClass().getDeclaredFields().filter(field => field.getName() != "MODULE$" )
    val counterRequestBuilder = Counter.build()
        .name(counterProperties.name)
        .help(counterProperties.help)
    if (!labelNames.isEmpty) {
      counterRequestBuilder
        .labelNames(
          labelNames
            .sortBy(field => field.getName())
            .map(field => field.getName())
          : _*)
    }
    val requestUserCounter: Counter = counterRequestBuilder.register(registry)
    registeredCounter += (MetricsProperties(counter).name -> requestUserCounter)
    requestUserCounter
  }

  // incrementUserCounter handle a prometheus counter increment
  def incrementUserCounter(counter: RegisterMetrics, prometheusCounter: Counter, by: Option[Long]) {
    val increment = by.getOrElse(1L)

    // Load labelNames, will be empty for an Object class
    val labelNames = counter.getClass().getDeclaredFields().filter(field => field.getName() != "MODULE$" )
    if (labelNames.isEmpty) {
      // Increment the associated Prometheus counter 
      prometheusCounter
        .inc(increment) 
    }  else { 

      // Increment the associated Prometheus counter with the the public Class fields
      prometheusCounter
        .labels(
          labelNames
            .sortBy(field => field.getName())
            .map(field => {
                field.setAccessible(true)
                val value = field.get(counter).asInstanceOf[String]
                field.setAccessible(false)
                value
            })
          : _*)
        .inc(increment)
    }
  }
}
//#user-registry-actor
