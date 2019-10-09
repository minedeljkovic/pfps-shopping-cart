package shop.http.routes

import cats.effect.Sync
import cats.implicits._
import io.estatico.newtype.ops._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain.auth._
import shop.http.json._
import shop.http.json.protocol._
import shop.services.AuthService
import shop.http.auth.roles._

final case class UserRoutes[F[_]: Sync](
    authService: AuthService[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    // creates a new user
    case req @ POST -> Root / "users" =>
      req.decode[CreateUser] { newUser =>
        // TODO: Use Chimney for conversions
        val username = newUser.username.value.coerce[UserName]
        val password = newUser.password.value.coerce[Password]

        authService
          .newUser(username, password, UserRole)
          .flatMap(Created(_))
          .handleErrorWith {
            case UserNameInUse(u) => Conflict(u.value)
          }
      }

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}