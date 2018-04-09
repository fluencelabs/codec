import de.heikoseeberger.sbtheader.License
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbtcrossproject.crossProject

name := "codec"

scalacOptions in Compile ++= Seq("-Ypartial-unification", "-Xdisable-assertions")

javaOptions in Test ++= Seq("-ea")

skip in publish := true // Skip root project

val scalaV = scalaVersion := "2.12.5"

val commons = Seq(
  scalaV,
  version                   := "0.0.1",
  fork in Test              := true,
  parallelExecution in Test := false,
  organizationName          := "Fluence Labs Limited",
  organizationHomepage      := Some(new URL("https://fluence.ai")),
  startYear                 := Some(2017),
  licenses += ("AGPL-V3", new URL("http://www.gnu.org/licenses/agpl-3.0.en.html")),
  headerLicense := Some(License.AGPLv3("2017", organizationName.value)),
  bintrayOrganization := Some("fluencelabs"),
  publishMavenStyle := true,
  bintrayRepository := "releases"
)

commons

val kindProjector = addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")

val Cats1V = "1.1.0"
val ScodecBitsV = "1.1.5"
val CirceV = "0.9.3"
val ShapelessV = "2.3.+"

val chill = "com.twitter" %% "chill" % "0.9.2"

val ScalatestV = "3.0.+"
val ScalacheckV = "1.13.4"

val protobuf = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  ),
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )
)

enablePlugins(AutomateHeaderPlugin)

lazy val `codec-core` = crossProject(JVMPlatform, JSPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(FluenceCrossType)
  .in(file("core"))
  .settings(
    commons,
    kindProjector,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"   % Cats1V,
      "org.typelevel" %%% "cats-laws" % Cats1V % Test,
      "org.typelevel" %%% "cats-testkit" % Cats1V % Test,
      "com.github.alexarchambault" %%% "scalacheck-shapeless_1.13" % "1.1.8" % Test,
      "org.scalacheck"  %%% "scalacheck" % ScalacheckV % Test,
      "org.scalatest" %%% "scalatest"    % ScalatestV % Test
    )
  )
  .jsSettings(
    fork in Test := false
  )
  .enablePlugins(AutomateHeaderPlugin)

lazy val `codec-core-jvm` = `codec-core`.jvm
lazy val `codec-core-js` = `codec-core`.js

lazy val `codec-bits` = crossProject(JVMPlatform, JSPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(FluenceCrossType)
  .in(file("bits"))
  .settings(
    commons,
    libraryDependencies ++= Seq(
      "org.scodec"    %%% "scodec-bits" % ScodecBitsV,
      "org.scalacheck"  %%% "scalacheck" % ScalacheckV % Test,
      "org.scalatest" %%% "scalatest"    % ScalatestV % Test
    )
  )
  .jsSettings(
    fork in Test := false
  )
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`codec-core`)

lazy val `codec-bits-js` = `codec-bits`.js
lazy val `codec-bits-jvm` = `codec-bits`.jvm

lazy val `codec-circe` = crossProject(JVMPlatform, JSPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(FluenceCrossType)
  .in(file("circe"))
  .settings(
    commons,
    libraryDependencies ++= Seq(
      "io.circe"      %%% "circe-core"   % CirceV,
      "io.circe"      %%% "circe-parser" % CirceV,
      "org.scalatest" %%% "scalatest"    % ScalatestV % Test
    )
  )
  .jsSettings(
    fork in Test := false
  )
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`codec-core`)

lazy val `codec-circe-js` = `codec-circe`.js
lazy val `codec-circe-jvm` = `codec-circe`.jvm

lazy val `codec-kryo` = project
  .in(file("kryo"))
  .settings(
    commons,
    libraryDependencies ++= Seq(
      chill,
      "com.chuusai"   %% "shapeless"     % ShapelessV,
      "org.scalatest" %% "scalatest"    % ScalatestV % Test
    )
  )
  .dependsOn(`codec-core-jvm`)

lazy val `codec-protobuf` = crossProject(JVMPlatform, JSPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(FluenceCrossType)
  .in(file("codec/protobuf"))
  .settings(
    commons,
    protobuf
  )
  .jsSettings(
    fork in Test      := false,
    scalaJSModuleKind := ModuleKind.CommonJSModule
  )
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(`codec-core`)

lazy val `codec-protobuf-jvm` = `codec-protobuf`.jvm
lazy val `codec-protobuf-js` = `codec-protobuf`.js

