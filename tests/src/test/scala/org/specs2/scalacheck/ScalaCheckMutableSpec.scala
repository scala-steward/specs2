package org.specs2.scalacheck

import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}
import org.specs2.ScalaCheck
import org.specs2.control.Action
import org.specs2.execute.Result
import org.specs2.matcher.ResultMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.core.{Env, OwnEnv}
import org.specs2.specification.process.DefaultExecutor
import org.specs2.matcher.ActionMatchers.*
import language.adhocExtensions

class ScalaCheckMutableSpec(val env: Env) extends Specification with ScalaCheck with ResultMatchers with OwnEnv:

  "this property must fail (see #581)" >> {
    val action: Action[Result] =
      DefaultExecutor.executeFragments(s2"fail here $failingProperties")(ownEnv).map(_.executionResult).head

    action must beOk((r: Result) => r must beFailing)
  }

  val failingProperties: Properties = new Properties("Hello") {
    property("ints") = forAll(Gen.choose(0, 10))((i: Int) => i === i + 1)
  }
