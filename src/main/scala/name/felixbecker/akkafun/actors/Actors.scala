package name.felixbecker.akkafun.actors

import akka.actor.{Actor, ActorRef}
import akka.persistence.{PersistentActor, SnapshotOffer}
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


class TickActor(greetingActor: ActorRef) extends PersistentActor {


  var greetResponseCounter = 0

  def updateState(greetResponse: GreetResponse): Unit = {
    greetResponseCounter += 1
  }

  println(s"I am the tick actor ${self.path}")

  val snapshotInterval = 100

  var recoverStateUpdateCounter = 0
  var recoverSnapshotOfferCounter = 0

  override def receiveCommand: Receive = {
    case Tick =>
      greetingActor ! GreetRequest("Oliver")

    case g @ GreetResponse(greeting) =>
      persist(g){ g =>
        updateState(g)
        context.system.eventStream.publish(g)
        if(lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0){
          println("============ Saving snapshot!")
          saveSnapshot(greetResponseCounter)
        }
      }
      println(s"I (the tick actor) received a greet response: $greeting. Greeting responses processed: $greetResponseCounter")

  }

  override def receiveRecover: Receive = {
    case g: GreetResponse =>
      println(s"=============== State update received! $recoverStateUpdateCounter")
      updateState(g)
      recoverStateUpdateCounter += 1
    case SnapshotOffer(_, snapshot: Int) =>
      println(s"=============== Snapshot offer received! $recoverSnapshotOfferCounter")
      greetResponseCounter = snapshot
      recoverSnapshotOfferCounter += 1
  }


  override def persistenceId: String = "tick-actor-2"
}
