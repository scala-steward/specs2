package org.specs2
package guide

import execute.{ResultExecution, Result, AsResult}
import specification.core.Fragment
import specification.{AroundEach, Around}
import org.specs2.specification.create.{DefaultFragmentFactory}
import time.SimpleTimer

/*
TODO
 */
object PrintExecutionData extends UserGuidePage {
  def is = s2"""
### Print success data

If an example returns returns a `Success` we just print the example name on the console but it can be interesting to also
get some information about the data the example was executed with. In order to do that you can use the `updateExpected`
method and pass a non-empty string with your message: ${snippet {
    "this is an obvious example" ! {
      val i = 1
      (i must ===(1)).updateExpected("executed with " + i)
    }
  }}

This will print on the console:
```
[info] + this is an obvious example
[info] executed with 1
```

Let's use this method now to display the execution time for each example of a specification.

### Print execution time

Knowing that an example succeeded is fine but sometimes you want to display more information,
like the time spent executing the example for instance, or some other state before and after each example.

This can be done by using the `AroundEach` trait and updating the `Result` of the example execution with whatever you want to display: $${snippet{

import org.specs2.specification.*
import org.specs2.execute.*
import org.specs2.time.*

trait Timed extends AroundEach:
  def around[T : AsResult](t: =>T): Result =
    // use `ResultExecution.execute` to catch possible exceptions
    val (result, timer) = withTimer(ResultExecution.execute(AsResult(t)))
    // update the result with a piece of text which will be displayed in the console
    result.updateExpected("Execution time: "+timer.time)

/** measure the execution time of a piece of code */
def withTimer[T](t: =>T): (T, SimpleTimer) =
  val timer = (new SimpleTimer).start
  val result = t
  (result, timer.stop)
}}

When you execute a specification mixing the `Timed` trait you should see the timing of each example displayed in the console:

```
[info] TimedExecutionSpecification
[info]
[info] + example 1
[info] Execution time: 94 ms
[info] + example 2
[info] Execution time: 11 ms
```

This is just an example to give you some inspiration. Since displaying execution times is quite often useful
the same functionality is actually accessible with the [`showtimes` argument]($ConsoleOutput).

### With the example description

More generally, you can both use the example description and the example body to display custom messages.
One way to do this is by taking the advantage of the fact that a `Specification` is just a stream of `Fragments`: ${snippet {

    import org.specs2.specification.core.*
    import org.specs2.execute.*
    import org.specs2.time.*

    object extras:
      /** measure the execution time of a piece of code */
      def withTimer[T](t: =>T): (T, SimpleTimer) =
        val timer = (new SimpleTimer).start
        val result = t
        (result, timer.stop)

      // extend each example in a Specification with a measured time message
      extension (fs: Fragments)
        def showTimes: Fragments =
          fs.map {
            case f if Fragment.isExample(f) =>
              f.updateResult { r =>
                val (result, timer) = withTimer(ResultExecution.execute(AsResult(r)))
                result.updateExpected("Execution time for \"" + f.description.show + "\": " + timer.time)
              }
            case other => other
          }

      // example of use
      class HelloWorldSpec extends Specification:
        def is = s2"""

    This is a specification to check the 'Hello world' string

    The 'Hello world' string should
      contain 11 characters $$e1
      start with 'Hello' $$e2
      end with 'world' $$e3
    """.showTimes
  }}

"""
}
