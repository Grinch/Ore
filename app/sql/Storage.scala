package sql

import models.project.Channel
import slick.driver.MySQLDriver.api._

class Storage(db: Database) {

  val projects: TableQuery[Projects] = TableQuery[Projects]

  val channels: TableQuery[Channels] = TableQuery[Channels]

  val versions: TableQuery[Versions] = TableQuery[Versions]

  val teams: TableQuery[Teams] = TableQuery[Teams]

  val devs: TableQuery[Devs] = TableQuery[Devs]

  val setup = DBIO.seq(
    (versions.schema ++ teams.schema ++ projects.schema ++ devs.schema ++ channels.schema).create,

    devs ++= Seq(
      (1, "Spongie"),
      (2, "Dev1"),
      (3, "Dev2"),
      (4, "Dev3"),
      (5, "Dev4"),
      (6, "Dev5")
    ),

    teams ++= Seq(
      (1, "SpongePowered"),
      (2, "Team1"),
      (3, "Team2"),
      (4, "Team3"),
      (5, "Team4"),
      (6, "Team5")
    ),

    versions += (1, 1, "1.0.0"),

    channels += (1, 1, "Alpha", Channel.HEX_GREEN),

    projects += (1, "org.spongepowered.ore", "Ore", "The Minecraft Plugin Repository", "Spongie", 0, 0, 0)
  )

  val setupFuture = db.run(setup)

}