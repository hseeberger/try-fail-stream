/*
 * Copyright 2018 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rocks.heikoseeberger.tryfailstream
import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Merge, Source }
import akka.stream.{ ActorAttributes, ActorMaterializer, Materializer, Supervision }
import scala.util.{ Failure, Success }

object Main {

  final case class Foo(n: Int)
  final case class Bar(n: Int)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: Materializer   = ActorMaterializer()

    import system.dispatcher

//    val decider: Supervision.Decider = {
//
//    }

    val foos =
      Source
        .fromIterator(() => Iterator.from(0))
        .take(666)
        .map(Foo)

    val errors = // Notice the type: `Source[Nothing, _]`!
      Source
        .fromIterator(() => Iterator.from(0))
//        .filterNot(_ == 42)
        .map(Bar)
        .collect {
          case Bar(n) if n == 42 =>
            throw new IllegalStateException("42 is a special number and deserves better treatment!")
        }

    Source
      .combine(foos, errors)(Merge(_))
      .withAttributes(ActorAttributes.supervisionStrategy {
        case _: IllegalArgumentException => Supervision.Resume
        case _                           => Supervision.Stop // Don't forget the default!
      })
      .runForeach(println)
      .andThen {
        case Failure(cause) => println(s"FAILURE: $cause")
        case Success(_)     => println("Done")
      }
      .andThen {
        case _ => system.terminate()
      }
  }
}
