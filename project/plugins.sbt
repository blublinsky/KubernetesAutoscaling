resolvers += "Bintray Repository" at "https://dl.bintray.com/shmishleniy/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.17")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.7.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.10"

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
