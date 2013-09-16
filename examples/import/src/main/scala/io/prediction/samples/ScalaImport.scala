package io.prediction.samples

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import io.prediction.{Client, FutureAPIResponse}

case class ScalaImportConfig(
  apiurl: String = "http://localhost:8000",
  appkey: String = "",
  inputFile: String = "")

/**
  * Sample data import client using MovieLens 100k data set in Scala.
  *
  * Uses a producer-consumer pattern to synchronize requests.
  * The producer is the main program, and the consumer is a separate thread.
  *
  * @author The PredictionIO Team
  */
object ScalaImport {
  /** Control variables shared with the consumer thread */
  var allSent = false
  var allDone = false
  var totalSent = 0

  /** Maximum number of requests allowed in memory */
  val maxRequests = 1000

  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[ScalaImportConfig]("scala-import") {
      head("PredictionIO Data Import Tool in Scala", "0.0.1-SNAPSHOT")
      help("help") text("prints this usage text")
      arg[String]("<app key>") action { (x, c) =>
        c.copy(appkey = x)
      } text("app key")
      arg[String]("<input file>") action { (x, c) =>
      	c.copy(inputFile = x)
      } text("u.data file from MovieLens 100k data set")
      opt[String]("apiurl") action { (x, c) =>
        c.copy(apiurl = x)
      } text("API URL (default: http://localhost:8000)")
    }

    parser.parse(args, ScalaImportConfig()) map { scalaImportConfig =>
      /** Create a client with an app key */
      val client = new Client(scalaImportConfig.appkey)

      /** Data structure */
      val uids = collection.mutable.Set[String]()
      val iids = collection.mutable.Set[String]()
      val q = new LinkedBlockingQueue[FutureAPIResponse]()

      /** Start the consumer thread to synchronize sent requests */
      new Thread(new FutureExaminer(q)).start()

      /** Read input MovieLens data and send requests to API */
      try {
        /** Get API status */
        println(client.getStatus())

        for (line <- scala.io.Source.fromFile(scalaImportConfig.inputFile).getLines()) {
          /** Throttling */
          while (q.size() > maxRequests) {
            Thread.sleep(1)
          }

          /** Break the line up */
          val st = line.split("\t")

          /** The 1st field is User ID, the 2nd field is Item ID, and the 3rd field is rating */
          val uid = st(0)
          val iid = st(1)
          val rate = st(2).toInt

          /** Save User IDs and Item IDs for adding later */
          uids.add(uid)
          iids.add(iid)

          /** Identify as current User ID */
          client.identify(uid)

          /** Create all types of actions for testing purpose
            * Push async handler to queue for synchronization
            */
          q.put(client.userActionItemAsFuture(client.getUserActionItemRequestBuilder("view", iid)))
          q.put(client.userActionItemAsFuture(client.getUserActionItemRequestBuilder("like", iid)))
          q.put(client.userActionItemAsFuture(client.getUserActionItemRequestBuilder("dislike", iid)))
          q.put(client.userActionItemAsFuture(client.getUserActionItemRequestBuilder("conversion", iid)))
          q.put(client.userActionItemAsFuture(client.getUserActionItemRequestBuilder("rate", iid).rate(rate)))

          /** Increase counter */
          totalSent += 5

          /* Print status per 2000 requests */
          if (totalSent % 2000 == 0) {
            println(s"Sent $totalSent requests so far")
          }
        }

        /** Add User and Item IDs asynchronously */
        println(s"Sending ${uids.size} create User ID requests")
        for (uid <- uids) {
          q.put(client.createUserAsFuture(client.getCreateUserRequestBuilder(uid)))
          totalSent += 1
        }

        println(s"Sending ${iids.size} create Item ID requests")
        val itypes = Array("movies")
        for (iid <- iids) {
          q.put(
            client.createItemAsFuture(
              client.getCreateItemRequestBuilder(iid, itypes)
                .attribute("url", s"http://localhost/${iid}.html")
                .attribute("startT", "ignored")))
          totalSent += 1
        }

        /** Tell the future examiner that everything has been sent */
        allSent = true
      } catch {
        case e: Exception => println(s"Error: ${e.getMessage()}")
      }

      /** Wait for synchronization */
      while (!allDone) {
        Thread.sleep(1)
      }

      client.close()
    }
  }

  /** A simple consumer examining the HTTP request result code */
  class FutureExaminer(q: BlockingQueue[FutureAPIResponse]) extends Runnable {
    def run() {
      var consumed = 0
      while (!(allSent && consumed != totalSent)) {
        val result = q.take
        consumed += 1
        if (result.getStatus() != 201) {
          println(result.getMessage())
        }
      }
      allDone = true
    }
  }
}
