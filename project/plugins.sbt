addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.15")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.19")

addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.26")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.6.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.13.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

addSbtPlugin("com.lihaoyi" % "workbench" % "0.4.1")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.8.4"

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")