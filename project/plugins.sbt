addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.25")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.2.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.23")

addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.28")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.6.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.15.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.0"

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
