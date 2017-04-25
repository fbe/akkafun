package name.felixbecker.akkafun

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import name.felixbecker.akkafun.actors.{GreetingActor, TickActor}
import name.felixbecker.akkafun.messages.Tick

import scala.util.{Failure, Success}

class DebugActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case x => println(s"$x ${sender()}")
  }

}

class HelloWorldActor(instanceNumber: Int) extends Actor with ActorLogging {
  log.info(s"Hello, i am the HelloWorldActor[$instanceNumber], my path is ${self.path}")

  override def receive: Receive = {
    case x => log.error(s"Received unexpected message $x")
  }
}

object Main extends App {

  implicit val actorSystem = ActorSystem("akkafun")
  // context actor of für child
  implicit val executionContext = actorSystem.dispatcher

  // Akka remote:
  // ip adresse + host + port -> remote

  // Akka Cluster:
  // Aufsatz: knoten kennen sich, leader election

  // Cluster Singleton Pattern (jar)

  // Akka Cluster Sharding (setzt auf singleton)!

  // Akka Stash

  // Shutdown von Actors über Shardregion

  val applicationConfig = ConfigFactory.load()
  val httpBindHost = applicationConfig.getString("akka.http.server.host")
  import scala.concurrent.duration._
  import akka.util.Timeout

  // only spawn actors on the "first" node
  if(httpBindHost.endsWith("1")) {
    (1 to 100000).foreach { instance =>
      actorSystem.actorOf(Props(classOf[HelloWorldActor], instance), s"HelloWorldClusterActor-$instance")
    }
  } else if(httpBindHost.endsWith("2")){
    Thread.sleep(5000)
    actorSystem.actorOf(Props(classOf[HelloWorldActor], 1), "HelloWorldClusterActor-X4711")
    val actorPath = actorSystem / "HelloWorldClusterActor-X4711"
    println(s"Trying to resolve actor $actorPath")
    implicit val timeout = Timeout(5.seconds)
    actorSystem.actorSelection(actorPath).resolveOne()
      .onComplete {
        case Success(r) => r ! "Hello my little actor friend!"
        case Failure(e) => e.printStackTrace()
      }
  }








  if(false) {
    // old fun impl
    val greetActorRef = actorSystem.actorOf(Props[GreetingActor])
    val tickActorRef = actorSystem.actorOf(Props(classOf[TickActor], greetActorRef))
    val debugActorRef = actorSystem.actorOf(Props[DebugActor])

    import scala.concurrent.duration._
    actorSystem.scheduler.schedule(2.seconds, 1000.milliseconds, tickActorRef, Tick)


    // akka http stuff below

    implicit val materializer = ActorMaterializer()
    import akka.http.scaladsl.server.Directives._

    val route =
      path("hello") {
        get {

          debugActorRef ! "Palim"

          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }


    val bindingFuture = Http().bindAndHandle(route, httpBindHost, 8080)
  }
}
