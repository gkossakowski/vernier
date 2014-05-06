package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter
import play.libs.Akka
import actors._
import akka.actor._
import akka.actor.ActorRef
import play.api.Play.current
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object ScalaWebSockets extends Controller {

  private val objectMapper = new ObjectMapper
  private implicit val jsonNodeFormatter = 
    FrameFormatter.stringFrame.transform[JsonNode](
      json => objectMapper.writeValueAsString(json),
      str => objectMapper.readTree(str))
  def ws = WebSocketActor.actorOf[JsonNode] { _ => upstream =>
    Props.create(classOf[UserActor], upstream)
  }
}
