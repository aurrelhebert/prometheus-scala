package com.clevercloud.prometheus.metrics

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.clevercloud.prometheus.metrics.UserRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import io.prometheus.client.Counter

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  val requestRouteCounter: Counter = Counter.build()
     .name("my_awesome_counter_route").labelNames("route","verb").help("Total requests.").register(MetricsController.registry.underlying)

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
              requestRouteCounter.labels("users", "get").inc()
              complete(getUsers())
            },
            post {
              requestRouteCounter.labels("users", "post").inc()
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        //#users-get-delete
        //#users-get-post
        path(Segment) { name =>
          concat(
            get {
              requestRouteCounter.labels("single_user", "get").inc()
              //#retrieve-user-info
              rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                  complete(response.maybeUser)
                }
              }
              //#retrieve-user-info
            },
            delete {
              requestRouteCounter.labels("single_user", "delete").inc()
              //#users-delete-logic
              onSuccess(deleteUser(name)) { performed =>
                complete((StatusCodes.OK, performed))
              }
              //#users-delete-logic
            })
        })
      //#users-get-delete
    }
  //#all-routes
}
