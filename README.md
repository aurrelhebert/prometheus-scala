# Prometheus Metrics in Scala

The aim of this small project is to re-use the [AKKA HTTP exemple](https://github.com/akka/akka-http-quickstart-scala.g8) with the [AKKA metrics library](https://github.com/RustedBones/akka-http-metrics). This application exposes AKKA native Metrics as well as our own custom ones. Those metrics respects the Prometheus standard, and are available to be queried on "/metrics" of this application. This means that they can be scrapped by a lot of existing agent. 

## Running 

Start the application: 

```sh
sbt run
```

Get the metrics

```sh
curl 127.0.0.1:8080/metrics
```

## Prometheus metrics 

In term of code, we retrieve `MetricController` used to declare the Prometheus registry. In the `UserRegistry` and `UserRoutes` classes, we declare and increment two prometheus counters : `my_awesome_counter_user` and `my_awesome_counter_route`.

The `QuickstartApp` intergrates the use of the Prometheus registry, and the collect of native AKKA metrics (as we start the application HTTP server with the method `newMeteredServerAt` and the `MetricController` registry as parameter).

## License

This code is released with an [Apache 2 license](./LICENSE).