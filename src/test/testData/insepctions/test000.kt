package test

import react.*

interface <weak_warning descr="Missed builder function">MyComponentProps</weak_warning> : RProps {
  var x: Int
}

interface MyComponentState : RState

class <warning descr="Props not passed to constructor"><weak_warning descr="Missed builder function">MyComponent</weak_warning></warning> : RComponent<MyComponentProps, MyComponentState>() {
  override fun RBuilder.render() {
  }
}