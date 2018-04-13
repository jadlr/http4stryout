import sbt._

object Dependencies {

  private val CatsVersion       = "1.1.0"
  private val CatsEffectVersion = "0.10"
  private val Http4sVersion     = "0.18.7"
  private val MonixVersion      = "3.0.0-RC1"
  private val CirceVersion      = "0.9.3"
  private val PureConfigVersion = "0.9.1"
  private val ScalacacheVersion = "0.23.0"
  private val LogbackVersion    = "1.2.3"

  lazy val runtimeDeps: Seq[ModuleID] = Seq(
    "org.typelevel"         %% "cats-core"              % CatsVersion,
    "org.typelevel"         %% "cats-effect"            % CatsEffectVersion,
    "org.http4s"            %% "http4s-blaze-server"    % Http4sVersion,
    "org.http4s"            %% "http4s-blaze-client"    % Http4sVersion,
    "org.http4s"            %% "http4s-circe"           % Http4sVersion,
    "org.http4s"            %% "http4s-dsl"             % Http4sVersion,
    "io.monix"              %% "monix"                  % MonixVersion,
    "io.circe"              %% "circe-core"             % CirceVersion,
    "io.circe"              %% "circe-generic"          % CirceVersion,
    "io.circe"              %% "circe-parser"           % CirceVersion,
    "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
    "com.github.cb372"      %% "scalacache-caffeine"    % ScalacacheVersion,
    "com.github.cb372"      %% "scalacache-cats-effect" % ScalacacheVersion,
    "ch.qos.logback"        %  "logback-classic"        % LogbackVersion
  )

  lazy val testDeps: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5"
  ).map(_ % Test)

}
