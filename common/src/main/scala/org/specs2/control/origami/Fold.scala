package org.specs2
package control
package origami

import org.specs2.fp.*
import org.specs2.fp.syntax.*

/** A Fold is a "left fold" over a data structure with:
  *   - a 'start' value
  *   - a 'fold' method to accumulate state
  *   - an 'end' method to finalize the result
  *
  * Both 'start' and 'end' have an effect which allows the whole folding to take place inside a context.
  */
trait Fold[M[_], A, B]:
  self =>

  def monad: Monad[M]

  given Monad[M] = monad

  type S

  def start: M[S]
  def fold: (S, A) => M[S]
  def end(s: S): M[B]

  /** map the output value */
  def map[C](f: B => C): Fold[M, A, C] =
    new Fold[M, A, C]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = self.start
      def fold = self.fold
      def end(s: S) = self.end(s).map(f)

  /** flatMap the output value */
  def mapFlatten[C](f: B => M[C]): Fold[M, A, C] =
    new Fold[M, A, C]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = self.start
      def fold = self.fold
      def end(s: S) = monad.bind(self.end(s))(f)

  /** run another fold on the end result */
  def pipe[C](f: Fold[M, B, C]): Fold[M, A, C] =
    new Fold[M, A, C]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = self.start
      def fold = self.fold
      def end(s: S) = self.end(s).flatMap(f.run1)

  /** parallel composition */
  def ***[V, W](f: Fold[M, V, W]): Fold[M, (A, V), (B, W)] =
    new Fold[M, (A, V), (B, W)]:
      type S = (self.S, f.S)
      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, f.start)
      def fold = (s: S, av: (A, V)) => monad.tuple2(self.fold(s._1, av._1), f.fold(s._2, av._2))
      def end(s: S) = monad.tuple2(self.end(s._1), f.end(s._2))

  /** fanout = zip in the Arrow terminology */
  def &&&[C](f: Fold[M, A, C]): Fold[M, A, (B, C)] =
    zip(f)

  /** contramap the input values */
  def contramap[C](f: C => A): Fold[M, C, B] =
    new Fold[M, C, B]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = self.start
      def fold = (s: S, c: C) => self.fold(s, f(c))
      def end(s: S) = self.end(s)

  /** contramap the input values with effects */
  def contraflatMap[C](f: C => M[A]): Fold[M, C, B] =
    new Fold[M, C, B]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = self.start
      def fold = (s: S, c: C) => monad.bind(f(c))(a => self.fold(s, a))
      def end(s: S) = self.end(s)

  /** zip 2 folds to return a pair of values. alias for zip */
  def <*>[C](f: Fold[M, A, C]): Fold[M, A, (B, C)] =
    zip(f)

  /** zip 2 folds to return a pair of values. alias for <*> */
  def zip[C](f: Fold[M, A, C]) =
    new Fold[M, A, (B, C)]:
      type S = (self.S, f.S)

      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, f.start)
      def fold = (s, a) => monad.tuple2(self.fold(s._1, a), f.fold(s._2, a))
      def end(s: S) = monad.tuple2(self.end(s._1), f.end(s._2))

  /** zip with another fold, running this one only for its side effects */
  def *>[C](f: Fold[M, A, C]): Fold[M, A, C] =
    zip(f).map(_._2)

  /** alias for *> */
  def observedBy[C](f: Fold[M, A, C]): Fold[M, A, C] =
    zip(f).map(_._2)

  /** zip with another fold only for its side effects */
  def <*[C](f: Fold[M, A, C]): Fold[M, A, B] =
    zip(f).map(_._1)

  /** alias for <* */
  def observe[C](f: Fold[M, A, C]): Fold[M, A, B] =
    zip(f).map(_._1)

  /** observe both the input value and the current state */
  def observeWithState(sink: Sink[M, (A, S)]): Fold[M, A, B] =
    new Fold[M, A, B]:
      type S = (self.S, sink.S)
      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, sink.start)
      def fold = (s: S, a: A) => monad.tuple2(self.fold(s._1, a), sink.fold(s._2, (a, s._1)))
      def end(s: S) = monad.tuple2(self.end(s._1), sink.end(s._2)).map(_._1)

  /** alias for observeWithState */
  def <<-*(sink: Sink[M, (A, S)]) =
    observeWithState(sink)

  /** observe the current state */
  def observeState(sink: Sink[M, S]): Fold[M, A, B] =
    new Fold[M, A, B]:
      type S = (self.S, sink.S)
      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, sink.start)
      def fold = (s: S, a: A) => monad.tuple2(self.fold(s._1, a), sink.fold(s._2, s._1))
      def end(s: S) = monad.tuple2(self.end(s._1), sink.end(s._2)).map(_._1)

  /** alias for observeState */
  def <-*(sink: Sink[M, S]) =
    observeState(sink)

  /** observe both the input value and the next state */
  def observeWithNextState(sink: Sink[M, (A, S)]): Fold[M, A, B] =
    new Fold[M, A, B]:
      type S = (self.S, sink.S)
      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, sink.start)
      def fold = (s: S, a: A) => self.fold(s._1, a).flatMap(next => sink.fold(s._2, (a, next)).map((next, _)))
      def end(s: S) = monad.tuple2(self.end(s._1), sink.end(s._2)).map(_._1)

  /** alias for observeWithNextState */
  def <<+*(sink: Sink[M, (A, S)]) =
    observeWithNextState(sink)

  /** observe the next state */
  def observeNextState(sink: Sink[M, S]): Fold[M, A, B] =
    new Fold[M, A, B]:
      type S = (self.S, sink.S)
      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, sink.start)
      def fold = (s: S, a: A) => self.fold(s._1, a).flatMap(next => sink.fold(s._2, next).map((next, _)))
      def end(s: S) = monad.tuple2(self.end(s._1), sink.end(s._2)).map(_._1)

  /** alias for observeNextState */
  def <+*(sink: Sink[M, S]) =
    observeNextState(sink)

  /** run a Fold with a Foldable instance
    */
  def run[F[_]: Foldable](foldable: F[A]): M[B] =
    foldable.toList.foldLeft(start) { (res, cur) => res.flatMap(r => fold(r, cur)) }.flatMap(end)

  /** run over one element
    */
  def run1(a: A): M[B] =
    start.flatMap(s => fold(s, a).flatMap(end))

  /** pipe the output of this fold into another fold */
  def compose[C](f2: Fold[M, B, C]): Fold[M, A, C] =
    new Fold[M, A, C]:
      type S = (self.S, f2.S)
      val monad: Monad[M] = self.monad

      def start = monad.tuple2(self.start, f2.start)

      def fold = (s, a) =>
        self.fold(s._1, a).flatMap(self.end).flatMap((u: B) => monad.tuple2(self.fold(s._1, a), f2.fold(s._2, u)))

      def end(s: S) =
        f2.end(s._2)

  /** create a fold that will run this fold repeatedly on input elements and collect all results */
  def nest[F[_], C](f: C => F[A])(using monoid: Monoid[B], foldable: Foldable[F]): Fold[M, C, B] =
    new Fold[M, C, B]:
      type S = B
      val monad: Monad[M] = self.monad

      def start = monad.pure(monoid.zero)

      def fold = (s: S, c: C) => self.run(f(c)).map((b: B) => monoid.append(s, b))

      def end(s: S) = monad.pure(s)

  /** equivalent of the as method for functors, added here for easier type inference */
  def as[C](c: =>C): Fold[M, A, C] =
    map(_ => c)

  /** equivalent of the void method for functors, added here for easier type inference */
  def void: Fold[M, A, Unit] =
    as(())

  def startWith(action: M[Unit]): Fold[M, A, B] { type S = self.S } =
    new Fold[M, A, B]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = action >> self.start
      def fold = (s, a) => self.fold(s, a)
      def end(s: S) = self.end(s)

  def endWith(action: M[Unit]): Fold[M, A, B] { type S = self.S } =
    new Fold[M, A, B]:
      type S = self.S
      val monad: Monad[M] = self.monad

      def start = self.start
      def fold = (s, a) => self.fold(s, a)
      def end(s: S) = self.end(s).flatMap(b => action.as(b))

  def into[M1[_]](using nat: M ~> M1, m: Monad[M1]) =
    monadic[M1](using nat, m)

  def monadic[M1[_]](using nat: M ~> M1, m: Monad[M1]) =
    new Fold[M1, A, B]:
      type S = self.S
      val monad: Monad[M1] = m

      def start = nat(self.start)
      def fold = (s, a) => nat(self.fold(s, a))
      def end(s: S) = nat(self.end(s))

end Fold

object Fold:

  given [M[_], A](using m: Monad[M]): Monoid[Fold[M, A, Unit]] with
    def zero =
      Folds.fromStart(m.point(()))

    def append(s1: Fold[M, A, Unit], s2: =>Fold[M, A, Unit]): Fold[M, A, Unit] = new Fold[M, A, Unit] {
      val monad: Monad[M] = m

      lazy val s2_ = s2
      type S = (s1.S, s2_.S)

      def start = s1.start.flatMap(s1s => s2_.start.map(s2s => (s1s, s2s)))
      def fold = (s: S, a: A) => s1.fold(s._1, a).flatMap(s11 => s2_.fold(s._2, a).map(s22 => (s11, s22)))
      def end(s: S) = s1.end(s._1) >> s2_.end(s._2)
    }

  /** Applicative instance
    *
    * This means that we can write:
    *
    * val mean: Fold[Int, Int] = (sum |@| count)(_ / _)
    *
    * An Applicative instance is also a Functor instance so we can write:
    *
    * val meanTimes2 = mean.map(_ * 2)
    */
  given A[M[_], T](using m: Monad[M]): Applicative[Fold[M, T, *]] with
    type F[U] = Fold[M, T, U]

    def point[A](a: =>A): Fold[M, T, A] =
      new Fold[M, T, A] {
        type S = Unit
        val monad: Monad[M] = m

        def start: M[S] = monad.point(())
        def fold: (S, T) => M[S] = (s, t) => monad.point(s)
        def end(s: S): M[A] = monad.point(a)
      }

    override def map[A, B](fa: F[A])(f: A => B): F[B] =
      fa `map` f

    def ap[A, B](fa: =>F[A])(f: =>F[A => B]): F[B] =
      map(fa `zip` f) { case (a, b) => b(a) }

/** Typeclass instances and creation methods for folds
  */
trait Folds:

  /** @return a fold which uses a Monoid to accumulate elements */
  def fromMonoidMap[M[_], A, O: Monoid](f: A => O)(using m: Monad[M]): Fold[M, A, O] { type S = O } =
    new Fold[M, A, O]:
      type S = O
      val monad: Monad[M] = m

      def start = monad.point(Monoid[O].zero)
      def fold = (s: S, a: A) => monad.point(Monoid[O].append(s, f(a)))
      def end(s: S) = monad.point(s)

  /** @return a fold from arguments of a fold left */
  def fromFoldLeft[M[_], A, B](b: B)(f: (B, A) => M[B])(using m: Monad[M]): Fold[M, A, B] { type S = B } =
    new Fold[M, A, B]:
      type S = B
      val monad: Monad[M] = m

      def start = monad.point(b)
      def fold = (s: S, a: A) => f(s, a)
      def end(s: S) = monad.point(s)

  /** @return a fold with just a start action */
  def fromStart[M[_], A, S1](action: M[S1])(using m: Monad[M]) =
    new Fold[M, A, S1]:
      type S = S1
      val monad: Monad[M] = m

      def start = action
      def fold = (s: S, a: A) => monad.point(s)
      def end(s: S) = monad.point(s)

  def bracket[A, C](open: Action[C])(step: (C, A) => Action[C])(close: C => Finalizer): Fold[Action, A, Unit] =
    new Fold[Action, A, Unit]:
      type S = C
      val monad: Monad[Action] = Action.ActionMonad

      def start = open
      def fold = (s: S, a: A) => step(s, a).addLast(close(s))
      def end(s: S) = monad.point(close(s).run())

  def fromSink[M[_], A](action: A => M[Unit])(using m: Monad[M]): Fold[M, A, Unit] =
    new Fold[M, A, Unit]:
      type S = Unit
      val monad: Monad[M] = m

      def start = monad.point(())
      def fold = (s: S, a: A) => action(a)
      def end(s: S) = monad.point(())

  /** @return a fold which uses a Monoid to accumulate elements */
  def fromMonoidMapEval[M[_], A, O: Monoid](f: A => M[O])(using m: Monad[M]): Fold[M, A, O] { type S = O } =
    new Fold[M, A, O]:
      type S = O
      val monad: Monad[M] = m

      def start = monad.point(Monoid[O].zero)
      def fold = (s: S, a: A) => f(a).map(a1 => Monoid[O].append(s, a1))
      def end(s: S) = monad.point(s)

  /** @return a Fold which simply accumulates elements into a List */
  def list[A]: Fold[Id, A, List[A]] =
    new Fold[Id, A, List[A]]:
      // a ListBuffer is used for efficient appends
      val monad: Monad[Id] = Monad.idMonad

      type S = scala.collection.mutable.ListBuffer[A]
      def start = monad.point(new scala.collection.mutable.ListBuffer[A])
      def fold = (s: S, a: A) => monad.point { s.append(a); s }
      def end(s: S) = monad.point(s.toList)

end Folds

object Folds extends Folds
