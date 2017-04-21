package name.felixbecker.akkafun

import akka.actor.{ActorSystem, Props}
import name.felixbecker.akkafun.actors.{GreetingActor, TickActor}
import name.felixbecker.akkafun.messages.Tick

object Main extends App {

  val actorSystem = ActorSystem("test-actor-system")
  import actorSystem.dispatcher

  val greetActorRef = actorSystem.actorOf(Props[GreetingActor])
  val tickActorRef = actorSystem.actorOf(Props(classOf[TickActor], greetActorRef))

  import scala.concurrent.duration._
  actorSystem.scheduler.schedule(2.seconds, 1000.milliseconds, tickActorRef, Tick)


}
