package controllers

import play.api._
import play.api.mvc._
import com.fasterxml.jackson.databind.JsonNode;
//import play.mvc.WebSocket;
import play.libs.Akka;
import actors._;
import akka.actor._;
import akka.actor.ActorRef;
import play.libs.F;

object Application extends Controller {

  def beaconInfo = Action { request =>
  	val paramsBody: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded

  	paramsBody map { (params: Map[String, Seq[String]]) =>
  		val beaconMajor = params("beaconMajor").head.toLong
  		val beaconMinor = params("beaconMinor").head.toLong
  		val rssi = params("beaconRSSI").head.toInt
  		val msg = s"ACK! beaconMajor=$beaconMajor,beaconMinor=$beaconMinor,rssi=$rssi"
      val beaconId = s"${beaconMajor}_$beaconMinor"
      StocksActor.stocksActor ! UpdateRSSI(beaconId, rssi)
  		println(msg)
  		Ok(msg)
  	} getOrElse BadRequest("Expecting application/form-url-encoded request body")
  }

}
