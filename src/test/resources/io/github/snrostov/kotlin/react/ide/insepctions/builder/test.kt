package react

open class RComponent<P, S>
interface RProps
interface RState

interface MyComponentProps : RProps {
  var x: Int
}
interface MyComponentState : RState

class MyComponent : RComponent<MyComponentProps, MyComponentState>()