package actors;

import akka.actor.UntypedActor;
import akka.actor.ActorRef;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

/**
 * The broker between the WebSocket and the StockActor(s).  The UserActor holds the connection and sends serialized
 * JSON data to the client.
 */

public class UserActor extends UntypedActor {

    private final ActorRef upstream;

    public UserActor(ActorRef upstream) {
        this.upstream = upstream;

        // watch the default stocks
        // List<String> defaultStocks = Play.application().configuration().getStringList("default.stocks");
        //
        // for (String stockSymbol : defaultStocks) {
        //     StocksActor.stocksActor().tell(new WatchStock(stockSymbol), getSelf());
        // }
        StocksActor.stocksActor().tell(FetchAllStockSymbols$.MODULE$, getSelf());
    }

    public void onReceive(Object message) {
        if (message instanceof JsonNode) {
          JsonNode jsonNode = (JsonNode)message;
          // parse the JSON into WatchStock
          WatchStock watchStock = new WatchStock(jsonNode.get("symbol").textValue());
          // send the watchStock message to the StocksActor
          StocksActor.stocksActor().tell(watchStock, getSelf());
        } else if (message instanceof StockUpdate) {
            // push the stock to the client
            StockUpdate stockUpdate = (StockUpdate)message;
            ObjectNode stockUpdateMessage = Json.newObject();
            stockUpdateMessage.put("type", "stockupdate");
            stockUpdateMessage.put("symbol", stockUpdate.symbol());
            stockUpdateMessage.put("price", stockUpdate.price().doubleValue());
            upstream.tell(stockUpdateMessage, getSelf());
        }
        else if (message instanceof StockHistory) {
            // push the history to the client
            StockHistory stockHistory = (StockHistory)message;

            ObjectNode stockUpdateMessage = Json.newObject();
            stockUpdateMessage.put("type", "stockhistory");
            stockUpdateMessage.put("symbol", stockHistory.symbol());

            ArrayNode historyJson = stockUpdateMessage.putArray("history");
            for (Object price : stockHistory.history()) {
                historyJson.add(((Number)price).doubleValue());
            }

            upstream.tell(stockUpdateMessage, getSelf());
        } else if (message instanceof AllStockSymbols) {
          AllStockSymbols allStock = (AllStockSymbols)message;
          for (String symbol : allStock.symbols()) {
            System.out.println("Watching " + symbol);
            StocksActor.stocksActor().tell(new WatchStock(symbol), getSelf());
          }
        }
    }
}
