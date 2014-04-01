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

class StockActor(symbol: String) extends Actor {

  lazy val stockQuote: StockQuote = new FakeStockQuote

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]



  // A random data set which uses stockQuote.newPrice to get each data point
  var stockHistory: Queue[java.lang.Double] = {
    //lazy val initialPrices: Stream[java.lang.Double] = (new Random().nextDouble * 800) #:: initialPrices.map(previous => stockQuote.newPrice(previous))
    Queue.fill(50)(-74)
  }

  // Fetch the latest stock value every 75ms
  //val stockTick = context.system.scheduler.schedule(Duration.Zero, 200.millis, self, FetchLatest)

  def receive = {
    case updateRSSI @ UpdateRSSI(symbol: String, rssi: Int) =>
      assert(this.symbol == symbol, s"${this.symbol} != $symbol")
      println(s"Received $updateRSSI")
      stockHistory = stockHistory.drop(1) :+ new java.lang.Double(rssi)
      // notify watchers
      watchers.foreach(_ ! StockUpdate(symbol, rssi))
/*    case FetchLatest =>
      // add a new stock price to the history and drop the oldest
      val newPrice = stockQuote.newPrice(stockHistory.last.doubleValue())
      stockHistory = stockHistory.drop(1) :+ newPrice
      // notify watchers
      watchers.foreach(_ ! StockUpdate(symbol, newPrice))*/
    case msg@WatchStock(_) =>
      println(msg)
      // send the stock history to the user
      sender ! StockHistory(symbol, stockHistory.asJava)
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
        context.actorOf(Props(new StockActor(symbol)), symbol)
      } forward updateRSSI
    case watchStock @ WatchStock(symbol) =>
      // get or create the StockActor for the symbol and forward this message
      context.child(symbol).getOrElse {
        context.actorOf(Props(new StockActor(symbol)), symbol)
      } forward watchStock
    case unwatchStock @ UnwatchStock(Some(symbol)) =>
      // if there is a StockActor for the symbol forward this message
      context.child(symbol).foreach(_.forward(unwatchStock))
    case unwatchStock @ UnwatchStock(None) =>
      // if no symbol is specified, forward to everyone
      context.children.foreach(_.forward(unwatchStock))
    case FetchAllStockSymbols =>
      println(s"61955_13474: ${context.child("61955_13474").isDefined}, self = $self")
      println(s"Children size = ${context.children.size}")
      val symbols = context.children.toList.map(_.path.name).toList
      println(s"All children are $symbols")
      sender ! AllStockSymbols(symbols.asJava)
  }
}

object StocksActor {
  lazy val stocksActor: ActorRef = Akka.system.actorOf(Props(classOf[StocksActor]))
}


case object FetchLatest

case object FetchAllStockSymbols

case class AllStockSymbols(symbols: java.util.List[String])

case class StockUpdate(symbol: String, price: Number)

case class StockHistory(symbol: String, history: java.util.List[java.lang.Double])

case class WatchStock(symbol: String)

case class UpdateRSSI(symbol: String, rssi: Int)

case class UnwatchStock(symbol: Option[String])
