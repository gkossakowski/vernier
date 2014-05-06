package actors

import akka.actor.Actor
import akka.actor.ActorRef
import com.fasterxml.jackson.databind.JsonNode
import play.libs.Json
import scala.collection.JavaConverters._

class UserActor(upstream: ActorRef) extends Actor {
  StocksActor.stocksActor ! FetchAllStockSymbols
  def receive = {
    case jsonNode: JsonNode =>
      // parse the JSON into WatchStock
      val watchStock = new WatchStock(jsonNode.get("symbol").textValue());
      // send the watchStock message to the StocksActor
      StocksActor.stocksActor ! watchStock
    case stockUpdate: StockUpdate =>
      // push the stock to the client
      val stockUpdateMessage = Json.newObject()
      stockUpdateMessage.put("type", "stockupdate");
      stockUpdateMessage.put("symbol", stockUpdate.symbol)
      stockUpdateMessage.put("price", stockUpdate.price.doubleValue)
      upstream ! stockUpdateMessage
    // push the history to the client
    case stockHistory: StockHistory =>
      val stockUpdateMessage = Json.newObject();
      stockUpdateMessage.put("type", "stockhistory")
      stockUpdateMessage.put("symbol", stockHistory.symbol)
      val historyJson = stockUpdateMessage.putArray("history")
      for (price <- stockHistory.history.asScala) {
        historyJson.add((price).doubleValue)
      }
      upstream ! stockUpdateMessage
    case allStock: AllStockSymbols =>
      for (symbol <- allStock.symbols.asScala) {
            System.out.println("Watching " + symbol)
            StocksActor.stocksActor ! new WatchStock(symbol)
      }
  }
}
