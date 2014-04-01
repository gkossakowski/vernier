package controllers;

import actors.*;
import akka.actor.*;
import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.Option;


/**
 * The main web controller that handles returning the index page, setting up a WebSocket, and watching a stock.
 */
public class WebSockets extends Controller {

    // this method is here just because mixing Java and Scala on the single template
    // turns out to be very difficult
    public static Result index() {
        return ok(views.html.index.render());
    }

    public static WebSocket<JsonNode> ws() {
        return new WebSocket<JsonNode>() {
            public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
                // create a new UserActor and give it the default stocks to watch
                final ActorRef userActor = Akka.system().actorOf(Props.create(UserActor.class, out));

                // send all WebSocket message to the UserActor
                in.onMessage(new F.Callback<JsonNode>() {
                    public void invoke(JsonNode jsonNode) throws Throwable {
                        // parse the JSON into WatchStock
                        WatchStock watchStock = new WatchStock(jsonNode.get("symbol").textValue());
                        // send the watchStock message to the StocksActor
                        StocksActor.stocksActor().tell(watchStock, userActor);
                    }
                });

                // on close, tell the userActor to shutdown
                in.onClose(new F.Callback0() {
                    public void invoke() throws Throwable {
                        final Option<String> none = Option.empty();
                        StocksActor.stocksActor().tell(new UnwatchStock(none), userActor);
                        Akka.system().stop(userActor);
                    }
                });
            }
        };
    }

}
