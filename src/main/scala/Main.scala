import akka.actor.{Actor, ActorRef, ActorSystem, Props}
/**
  * Created by becker on 4/21/17.
  */

case class GreetRequest(name: String)
case class GreetResponse(greeting: String)
case object Tick
class GreetingActor extends Actor {
  override def receive: Receive = {
    case GreetRequest(name) =>
      println(s"I received a greeting request for name $name")
      sender() ! GreetResponse(s"Hello $name")
  }
}

class TickActor(greetingActor: ActorRef) extends Actor {

  var greetResponseCounter = 0

  println(s"I am the tick actor ${self.path}")

  override def receive: Receive = {
    case Tick =>
      println("Tick tock!")
      greetingActor ! GreetRequest("Oliver")
    case GreetResponse(greeting) =>
      greetResponseCounter += 1
      println(s"I (the tick actor) received a greet response: $greeting. Greeting responses processed: $greetResponseCounter")

  }

}

object Main extends App {
  println("Oh Hai!")

  val actorSystem = ActorSystem("test-actor-system")
  import actorSystem.dispatcher

  val greetActorRef = actorSystem.actorOf(Props[GreetingActor])
  val tickActorRef = actorSystem.actorOf(Props(classOf[TickActor], greetActorRef))

  import scala.concurrent.duration._

  actorSystem.scheduler.schedule(0.milliseconds, 1.second, tickActorRef, Tick)



}
