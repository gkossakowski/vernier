package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def beaconInfo = Action { request =>
  	val paramsBody: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded

  	paramsBody map { (params: Map[String, Seq[String]]) =>
  		val beaconMajor = params("beaconMajor").head.toLong
  		val beaconMinor = params("beaconMinor").head.toLong
  		val rssi = params("beaconRSSI").head.toInt
  		val msg = s"ACK! beaconMajor=$beaconMajor,beaconMinor=$beaconMinor,rssi=$rssi"
  		println(msg)
  		Ok(msg)
  	} getOrElse BadRequest("Expecting application/form-url-encoded request body")
  }

}
