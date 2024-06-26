package org.specs2
package specification

import form.{given, *}
import Forms.{given, *}
import control.ImplicitParameters.*

trait ComponentsDefinitions:
  case class Address(street: String = "", number: Int = 0):
    def form =
      fill(street, number)

    def fill(s: String, n: Int): Form =
      Form("Address").tr(prop("street", s)(street)).tr(prop("number", n)(number))

  case class Customer(name: String = "", address: Address = Address()):
    def form =
      fill(name, address.form)

    def fill(na: String, a: Form): Form =
      Form("Customer").tr(prop("name", na)(name)).tr(a)

  case class initials(form: Form = Form.tr("First name", "Last name", "Initials")):
    def computeInitials(f: String, l: String): String =
      f.charAt(0).toUpper.toString + "." + l.charAt(0).toUpper.toString + "."

    def tr(firstName: String, lastName: String, expected: String) = initials {
      form.tr(firstName, lastName, prop(computeInitials(firstName, lastName))(expected))
    }

  case class Order(lines: List[OrderLine] = Nil):
    def line(orderLine: OrderLine) = Order(lines :+ orderLine)
    def hasSubset(ls: OrderLine*) = Form("Order").subset(lines, ls.toList)
    def hasSubsequence(ls: OrderLine*) = Form("Order").subsequence(lines, ls.toList)
    def hasSet(ls: OrderLine*) = Form("Order").set(lines, ls.toList)
    def hasSequence(ls: OrderLine*) = Form("Order").sequence(lines, ls.toList)
    def form = hasSubset(lines*)

  case class OrderLine(name: String, quantity: Int):
    def form =
      Form.tr(field("name", name), field("qty", quantity))

object ComponentsDefinitions extends ComponentsDefinitions
