package api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created on 2016-07-28.
  */
object GoogleProvider {

  val LoginAndroidId = "9774d56d682e549c"
  val LoginService = "audience:server:client_id:848232511240-7so421jotr2609rmqakceuu1luuq0ptb.apps.googleusercontent.com"
  val LoginApp = "com.nianticlabs.pokemongo"
  val LoginClientSignature = "321187995bc7cdc2b5fc91b11a96e2baa8602c62"

  case class ProviderSession(provider: String, token: String)

  def login(username: String, password: String)
           (implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Future[ProviderSession] = {
    for {
      googleAuthRes <- GoogleAuthClient.login(username, password, LoginAndroidId)
      session <- GoogleAuthClient.oauth(username, googleAuthRes.masterToken, LoginAndroidId, LoginService, LoginApp, LoginClientSignature)
    } yield ProviderSession("google", session.token)
  }
}
