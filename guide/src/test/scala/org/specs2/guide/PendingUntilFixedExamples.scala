package org.specs2
package guide

object PendingUntilFixedExamples extends UserGuidePage {
  def is = "Pending until fixed".title ^ s2"""

Some examples may be temporarily failing but you may not want the entire test suite to fail just for those examples. Instead of commenting them out and then forgetting about those examples when the code is fixed, you can append `pendingUntilFixed` to the example: ${snippet {

    class SpecificationWithPendingExamples extends mutable.Spec:
      "this example fails for now" >> pendingUntilFixed {
        1 must ===(2)
      }

      // or, with a more specific message
      "this example fails for now" >> pendingUntilFixed("ISSUE-123") {
        1 must ===(2)
      }
  }}

The example above will be reported as `Pending` until it succeeds. Then it is marked as a failure so that you can remember to remove the `pendingUntilFixed` marker.

"""
}
