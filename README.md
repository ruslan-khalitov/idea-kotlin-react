# Kotlin React Tools

## Can I use it?

Not yet.

## Installation & Usage

Currently the plugin is compatible only with latest dev builds of IntelliJ IDEA and Kotlin.
So, at this time you should build it yourself: clone this repo and call `./gradlew runIde`.

See [CONTRIBUTING.md] for more details.

## Features

_Click at ![Arrow](https://user-images.githubusercontent.com/2010355/37763367-e4e0068e-2dcf-11e8-8879-c215c6403662.png) to see the GIF._ 

<details><summary>Generating React Components boilerplate (generating `RProps interface`, builder function and render function)</summary>
<p>

![creating component](https://user-images.githubusercontent.com/2010355/37760359-7c69eb04-2dc7-11e8-942b-805757919720.gif)

</p>
</details>

<details><summary>Updating DSL builder function on editing `RProps` declarations</summary>
<p>

Plugin gracefully updates DSL builder function regardless of changes: adding, renaming and changing type.
Properties order are preserved.

![02-editing-rprops](https://user-images.githubusercontent.com/2010355/37762027-5372d2ec-2dcc-11e8-94a8-ecdf84d7bb7a.gif)

<details><summary>Even arbitrary builder function code supported</summary>
<p>

![03-builder-fun-cfg](https://user-images.githubusercontent.com/2010355/37762433-5286f66e-2dcd-11e8-94f8-1bbc3eb500b7.gif)

</p>
</details>

</p>
</details>

<details><summary>All `RBuilder.child(C::class) { ... }` calls are analyzed for missed `RProps` assignments:</summary>
<p>

![2018-03-22 12 35 10](https://user-images.githubusercontent.com/2010355/37762505-85d02d06-2dcd-11e8-85b9-eaa297abf83b.png)

</p>
</details>

<details><summary>Creating and inspecting `RState` initialization:</summary>
<p>

![04-creating-rstate](https://user-images.githubusercontent.com/2010355/37762608-db0a2308-2dcd-11e8-9f46-7b79ebc1f877.gif)

</p>
</details>

<details><summary>Removing/creating `RProps`/`RState`:</summary>
<p>

![05-remove-create-rprops-rstate](https://user-images.githubusercontent.com/2010355/37762669-1462779a-2dce-11e8-8f4d-66134d57b1ce.gif)

</p>
</details>

### Insepctions

`RBuilder.child(C::class) { /* lambda */ }` calls:
 - [x] Component builder function should be named as "..."
 - [x] Children is not initialized in component builder function
 - [x] Builder function has outdated parameter type
 - [x] Children is used in component, but not initialized
 - [x] All RProps vars should be initialized 
 - [x] Builder function contains outdated assignments

`RComponent` class declaration:
 - [x] Props not passed to constructor
 - [x] Missed builder function
 - [x] Outdated builder function
 - [x] Both "State.init" and "State.init(props)" is overridden
 - [x] Component has state that should be initialized
 - [x] All RState vars should be initialized
 - [x] `override fun State.init(props)`: RProps is not passed to component constructor
 - [x] on `children()` call: Children is not initialized in component builder function

`RProps`/`RState` interface declaration:
 - [ ] Only var properties are allowed in RProps/RState interfaces
 - [x] There are no props in RProps/RState interface
 - [x] Value is not initialized in component builder function
 - [x] Builder function contains outdated assignments
 - [x] Builder function contains outdated parameter type 
 - [x] Value is not initialized in component state init function

### Intentions and Quick fixes

`RComponent` class declaration:
 - [x] Generate Component builder function
 - [x] Actualize React Component builder function
 - [x] Add props constructor parameter

`RProps`/`RState` interface declaration:
 - [x] Delete empty `RProps`/`RState` interface
 - [x] Create `RProps`/`RState`
 - [x] Delete `RProps`/`RState` property
 - [x] Change `val` to `var`

### Code completion

 - [x] On typing `RComponent` super class name: create `RComponent` boilerplate
 	- `RProps` interface
 	- Builder function
 	- override `render` function
 - [ ] On typing `class MyComponent(props: ...`: suggest ...`: MyComponentProps): RComponent<MyComponentProps, RState> { ... }`

### Creating from usages

 - [ ] RProp property
 - [ ] RState property
 - [ ] RComponent

### Refactorings

 - [ ] Change RComponent signature (RProps)
 - [ ] Extract RComponent
 - [ ] Inline RComponent

### Actions
 
 - [ ] Project wizard
 - [ ] `HTML` to `Kotlin React` code
 - [ ] `RComponent` browser preview
 - [ ] `Surround with` / `Unwrap/remove` RComponent
 
### Other  

 - [ ] RComponent/RProps/RState/builder function gutter icons and navigation
 - [ ] Builder function folding
 - [ ] Emmet 

### File & Live templates

 - [ ] `rcomponent`: `RComponent` file & live template
 - [ ] `render`: function with `RBuilder` receiver
 
### Known bugs

 - [ ] Exception on generating `RComponent` boilerplate
 - [ ] `RComponent` boilerplate formatting
 - [ ] `RState` properties warnings not updated when typing in state init function
 
### Control flow graph analyzer limitions
 
 - [ ] Detect leaking `attrs`
 - [ ] Detect leaking `this` in state init functions
 - [ ] Analyze receiver data flow
    - [ ] Support `this.attrs.xxx`
    - [ ] Support `this.xxx` in state init functions
    - [ ] Support assignment to an arbitrary variable
    
[CONTRIBUTING.md]: CONTRIBUTING.md