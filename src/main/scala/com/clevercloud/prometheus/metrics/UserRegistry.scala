package com.clevercloud.prometheus.metrics

//#user-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import io.prometheus.client.Counter
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}

//#user-case-classes
final case class User(name: String, age: Int, countryOfResidence: String)
final case class Users(users: immutable.Seq[User])

//#user-case-classes

object UserRegistry {
  // actor protocol
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(name: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  // [NATIVE] use direclty the Prometheus library to declare a Counter metrics, (not required when using the metricsActor)
  final val requestUserCounter: Counter = Counter.build()
     .name("my_awesome_counter_user").help("Total requests.").register(MetricsController.registry.underlying)

  def apply(metricsActor: ActorRef[MetricsAction]): Behavior[Command] = registry(Set.empty, metricsActor)

  private def registry(users: Set[User], metricsActor: ActorRef[MetricsAction]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>

        // [NATIVE] use direclty the Prometheus library to increment a declared Counter metrics
        requestUserCounter.inc()

        // [ACTOR] use the metricsActor to increment a custom metrics Counter
        metricsActor.!(IncrementCounter(RegisterUserCounter))
        replyTo ! Users(users.toSeq)
        Behaviors.same
      case CreateUser(user, replyTo) =>
        replyTo ! ActionPerformed(s"User ${user.name} created.")
        registry(users + user, metricsActor)
      case GetUser(name, replyTo) =>
        replyTo ! GetUserResponse(users.find(_.name == name))
        Behaviors.same
      case DeleteUser(name, replyTo) =>
        replyTo ! ActionPerformed(s"User $name deleted.")
        registry(users.filterNot(_.name == name), metricsActor)
    }
}
//#user-registry-actor
