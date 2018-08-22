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
import akka.stream.{ ActorMaterializer, Materializer }
import scala.util.{ Failure, Success }

object Main {

  final case class Foo(n: Int)
  final case class Bar(n: Int)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val mat: Materializer   = ActorMaterializer()

    import system.dispatcher

    val foos =
      Source
        .fromIterator(() => Iterator.from(0))
        .map(Foo)

    val errors = // Notice the type: `Source[Nothing, _]`!
      Source
        .fromIterator(() => Iterator.from(0))
        .map(Bar)
        .collect {
          case Bar(n) if n == 42 =>
            throw new IllegalStateException("42 is a special number and deserves better treatment!")
        }

    Source
      .combine(foos, errors)(Merge(_))
      .runForeach(println)
      .onComplete {
        case Failure(cause) => println(s"FAILURE: $cause")
        case Success(_)     => println("Done")
      }
  }
}
