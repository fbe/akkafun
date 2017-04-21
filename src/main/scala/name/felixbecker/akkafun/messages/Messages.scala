package name.felixbecker.akkafun.messages

case class GreetResponse(greeting: String)
case class GreetRequest(name: String)
case object Tick
