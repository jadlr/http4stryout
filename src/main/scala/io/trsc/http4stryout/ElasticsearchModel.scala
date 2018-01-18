package io.trsc.http4stryout

import io.circe.Decoder

object ElasticsearchModel {

  case class Health(
    clusterName: String,
    status: String,
    timedOut: Boolean,
    numberOfNodes: Int
  )

  object Health {
    implicit val decoder: Decoder[Health] = Decoder.forProduct4(
      "cluster_name",
      "status",
      "timed_out",
      "number_of_nodes"
    )(Health.apply)
  }

}
