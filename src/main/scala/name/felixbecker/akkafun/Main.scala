package name.felixbecker.akkafun

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.config.ConfigFactory
import name.felixbecker.akkafun.HelloWorldActor.EndMessage


object PersistenceIds {
  val HelloWorldActorId = "HelloWorldActorSingleton"
}

object HelloWorldActor {
  case object EndMessage
}

class HelloWorldActor() extends Actor with ActorLogging with PersistentActor {

  log.info(s"Hello, i am the HelloWorldActor, my path is ${self.path}")

  var messageCounter: Int = 0

  def updateState(): Unit = {
    messageCounter += 1
  }

  override def receiveCommand: Receive = {
    case EndMessage =>
      log.info("Received an end message, stopping myself")
      context stop self
    case message: String =>
      persist(message) { m =>
        updateState()
        context.system.eventStream.publish(m) // TODO what happens if i disable this?
        log.info(s"Received a message (in receiveCommand, count is now $messageCounter): $message")
      }
  }

  override def receiveRecover: Receive = {
    case message: String =>
      log.info(s"State update (in receiveRecover) received: $message")
      updateState()

    case RecoveryCompleted => log.info("Yay, recovery completed!")
    case unexpected => log.error(s"Received unexpected message in receiveRecover $unexpected")
  }


  override def persistenceId: String = PersistenceIds.HelloWorldActorId
}

object Main extends App {

  implicit val actorSystem = ActorSystem("akkafun")
  // context actor of für child
  implicit val executionContext = actorSystem.dispatcher


  val applicationConfig = ConfigFactory.load()
  val httpBindHost = applicationConfig.getString("akka.http.server.host")

  val helloWorldActorSingletonManagerProps = ClusterSingletonManager.props(
    singletonProps = Props(classOf[HelloWorldActor]),
    terminationMessage = EndMessage,
    settings = ClusterSingletonManagerSettings(actorSystem)
  )

  val helloWorldActorClusterSingletonProxyProps = ClusterSingletonProxy.props(
    singletonManagerPath = "/user/helloWorldSingleton",
    settings = ClusterSingletonProxySettings(actorSystem)
  )


  actorSystem.actorOf(helloWorldActorSingletonManagerProps, name = "helloWorldSingleton")
  val helloWorldProxyRef = actorSystem.actorOf(helloWorldActorClusterSingletonProxyProps, name = "helloWorldSingletonProxy")

  // only spawn actors on the "first" node
  if(httpBindHost.endsWith("1")) { // 127.0.0.1
    helloWorldProxyRef ! "Greetings, this is 127.0.0.1"
  } else if(httpBindHost.endsWith("2")) { // 127.0.0.2
    helloWorldProxyRef ! "Greetings, this is 127.0.0.2"
  }



/*
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
  */
}
