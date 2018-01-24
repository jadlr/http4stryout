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


  case class Hit(id: String)
  object Hit {
    implicit val decoder: Decoder[Hit] = Decoder.forProduct1("_id")(Hit.apply)
  }

  case class Hits(total: Long, maxScore: Double, hits: List[Hit])
  object Hits {
    implicit val decoder: Decoder[Hits] = Decoder.forProduct3("total", "max_score", "hits")(Hits.apply)
  }

  case class Results(took: Long, timedOut: Boolean, hits: Hits)
  object Results {
    implicit val decoder: Decoder[Results] = Decoder.forProduct3("took", "timed_out", "hits")(Results.apply)
  }

}
