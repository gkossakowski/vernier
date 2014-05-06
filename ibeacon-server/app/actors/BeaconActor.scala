package actors

import akka.actor.{Props, ActorRef, Actor}
import utils.{StockQuote, FakeStockQuote}
import java.util.Random
import scala.collection.immutable.{HashSet, Queue}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.libs.Akka

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class BeaconActor(symbol: String) extends Actor {

  lazy val stockQuote: StockQuote = new FakeStockQuote

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  var beaconHistory: Queue[java.lang.Double] = {
    Queue.fill(50)(-74)
  }

  def receive = {
    case updateRSSI @ UpdateRSSI(symbol: String, rssi: Int) =>
      assert(this.symbol == symbol, s"${this.symbol} != $symbol")
      println(s"Received $updateRSSI")
      beaconHistory = beaconHistory.drop(1) :+ new java.lang.Double(rssi)
      // notify watchers
      watchers.foreach(_ ! RSSIUpdate(symbol, rssi))
    case msg@WatchBeacon(_) =>
      println(msg)
      // send the stock history to the user
      sender ! BeaconHistory(symbol, beaconHistory.asJava)
      // add the watcher to the list
      watchers = watchers + sender
    case msg@UnwatchStock(_) =>
      println(msg)
      watchers = watchers - sender
  }
}

class StocksActor extends Actor {
  def receive = {
    case updateRSSI @ UpdateRSSI(symbol, rssi) =>
      // get or create the StockActor for the symbol and forward this message
      context.child(symbol).getOrElse {
        println(s"Creating a new actor for $symbol, self = $self")
        context.actorOf(Props(new BeaconActor(symbol)), symbol)
      } forward updateRSSI
    case watchStock @ WatchBeacon(symbol) =>
      // get or create the StockActor for the symbol and forward this message
      context.child(symbol).getOrElse {
        context.actorOf(Props(new BeaconActor(symbol)), symbol)
      } forward watchStock
    case unwatchStock @ UnwatchStock(Some(symbol)) =>
      // if there is a StockActor for the symbol forward this message
      context.child(symbol).foreach(_.forward(unwatchStock))
    case unwatchStock @ UnwatchStock(None) =>
      // if no symbol is specified, forward to everyone
      context.children.foreach(_.forward(unwatchStock))
    case FetchAllBeaconSymbols =>
      println(s"61955_13474: ${context.child("61955_13474").isDefined}, self = $self")
      println(s"Children size = ${context.children.size}")
      val symbols = context.children.toList.map(_.path.name).toList
      println(s"All children are $symbols")
      sender ! AllBeaconSymbols(symbols.asJava)
  }
}

object StocksActor {
  lazy val stocksActor: ActorRef = Akka.system.actorOf(Props(classOf[StocksActor]))
}


case object FetchLatest

case object FetchAllBeaconSymbols

case class AllBeaconSymbols(symbols: java.util.List[String])

case class RSSIUpdate(symbol: String, rssi: Int)

case class BeaconHistory(symbol: String, history: java.util.List[java.lang.Double])

case class WatchBeacon(symbol: String)

case class UpdateRSSI(symbol: String, rssi: Int)

case class UnwatchStock(symbol: Option[String])
