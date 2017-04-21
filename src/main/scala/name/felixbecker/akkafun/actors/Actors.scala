package name.felixbecker.akkafun.actors

import akka.actor.{Actor, ActorRef}
import name.felixbecker.akkafun.messages.{GreetRequest, GreetResponse, Tick}

/**
  * Created by becker on 4/21/17.
  */
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
      greetingActor ! GreetRequest("Oliver")
    case GreetResponse(greeting) =>
      greetResponseCounter += 1
      println(s"I (the tick actor) received a greet response: $greeting. Greeting responses processed: $greetResponseCounter")

  }

}
