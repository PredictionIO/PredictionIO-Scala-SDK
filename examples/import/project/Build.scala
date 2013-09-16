import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {
  lazy val root = Project(
    id = "scala-import",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
    Seq(
      packMain := Map("scala-import" -> "io.prediction.samples.ScalaImport"),
      packJvmOpts := Map("scala-import" -> Seq("-Xmx512m")),
      packExtraClasspath := Map()
    )
  )
}
