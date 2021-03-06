resolvers ++= Seq(
    "Sonatype snapshots"                                 at "http://oss.sonatype.org/content/repositories/snapshots/",
    Classpaths.typesafeResolver,
    "jgit-repo"                                          at "http://download.eclipse.org/jgit/maven",
    "Twitter Repo"                                       at "http://maven.twttr.com/"
)

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.5.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.2")

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.15.0")

addSbtPlugin("com.twitter" % "sbt-package-dist" % "1.1.0")