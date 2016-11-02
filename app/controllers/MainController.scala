package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async {
    Future(Ok("Hi"))
  }

  def postFile(key: String) = play.mvc.Results.TODO

  def postMeta(key: String) = play.mvc.Results.TODO

}


