package config

import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.semiauto.deriveReader

final case class Config(db: DbConfig, app: AppConfig)
object Config {
  implicit val reader: ConfigReader[Config] = deriveReader

  def load: IO[Config] =
    IO.delay(ConfigSource.default.loadOrThrow[Config])
}

final case class DbConfig(
                           url: String,
                           driver: String,
                           user: String,
                           password: String
                         )
object DbConfig {
  implicit val reader: ConfigReader[DbConfig] = deriveReader
}

final case class AppConfig(host: String, port: String)
object AppConfig {
  implicit val reader: ConfigReader[AppConfig] = deriveReader
}
