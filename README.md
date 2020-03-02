# CS314-LinkedListView

This repository contains a debugging visualiser for the [`LinkedList` assignment](https://www.cs.utexas.edu/~scottm/cs314/Assignments/A5_LinkedLists.html) in CS 314 at the University of Texas at Austin.
Integrating this tool into your codebase is extremely simple, requiring you to change only a few lines of code in your tester file.
In return, you will get a full transcript of every linked list operation that you called, accompanied by colour-coded diagrammes detailing exactly what each operation changed about your list.

## Usage
To use this tool, either clone this repository using the following command:
```
git clone https://github.com/Andrew-William-Smith/CS314-LinkedListView.git
```
Or download the file `LinkedListView.java` directly from GitHub [here](https://raw.githubusercontent.com/Andrew-William-Smith/CS314-LinkedListView/master/LinkedListView.java).
Once you've downloaded the file, import it into your IDE workspace for the `LinkedList` project, in the same folder as `LinkedList.java` and `IList.java`.

Now that you've "installed" the visualiser, you'll have to make a small modification to your tester file to actually use it.
As `LinkedListView` inherits from `LinkedList`, the public interface is nearly identical save for the constructor, which provides some additional functionality required for visualisation.
As such, the only part of your code you should need to change is the declaration of a `LinkedList`.
`LinkedListView` must be declared inside a `try`-with-resources statement as follows:
```java
try (LinkedList<E> list = new LinkedListView<>("OutFile.html")) {
    // Test code here
} catch (NoSuchFieldException e) {
    // The structure of your LinkedList could not be analysed
}
```
Once you've written this statement and placed your tests inside the block labelled *Test code here*, you should be ready to go!
As a note, the file *OutFile.html* is where the `LinkedListView` writes its output and visualisations: you can name it whatever you'd like, but I recommend at least retaining the `.html` suffix so that the file will open in your web browser.
In addition, be careful not to redefine the variable in the `try` statement inside your tests: the Java compiler will detect a potential resource leak and throw you a compile error.

Once you run your code that uses a `LinkedListView`, you should see a file with the specified name appear in your root project folder.
You can open this file to see a log of all of the `LinkedList` operations your tests performed and diagrammes of all state changes that occurred as a result, which should look something like this:

![](https://raw.githubusercontent.com/Andrew-William-Smith/CS314-LinkedListView/master/examples/output-screenshot.png)

## Examples
For an example of how `LinkedListView` works and what its output looks like, see [`MikeTest.java`](https://raw.githubusercontent.com/Andrew-William-Smith/CS314-LinkedListView/master/examples/MikeTest.java) and one of the `MikeTest_*.html` files in the `examples` directory, respectively.
`MikeTest.java` is a very lightly adapted version of the default `LinkedList` test file that works with `LinkedListView`, `MikeTest-Circular.html` is the output of that `LinkedListView` for a properly implemented circular `LinkedList` with a "dummy" header node, and `MikeTest-Linear.html` is the output for a `LinkedList` with separate head and tail nodes.
If your output matches that file when viewed in a web browser, you should be good to go!

## Mechanics
You may be wondering, how exactly does this system work?  It seems to know a lot about your `LinkedList` implementation, but you didn't give it any information.
`LinkedListView` obtains its structural information about your class through *reflexion*, a programming technique that involves observing and modifying the structures of objects at runtime.
For example, to determine what your list's header node is called and if it has a tail node, `LinkedListView` examines the instance variables in your `LinkedList` class and matches upon any that contain the following substrings:

- *Header node:* `begin`, `first`, `front`, `head`, `init`
- *Tail node:* `end`, `final`, `last`, `tail`, `trail`

Furthermore, once it finds these nodes, it verifies their types to ensure that it will be able to successfully scan through your list without needing to create an `Iterator`.

`LinkedListView` also uses reflexion to access the private instance variables of `LinkedList` and your list node class: normally, Java does not allow subclasses to access their parents' private instance variables, so implementing this type of behaviour would be impossible.
However, through Java's reflexion API, `LinkedListView` is able to mark your classes' instance variables as `public` at runtime, allowing it to gain access to some implementation details that it otherwise couldn't observe.

If you get a `NoSuchFieldException` while attempting to construct a `LinkedListView`, that means that `LinkedListView` wasn't able to correctly identify your `LinkedList`'s header node or your list node class's instance variables.
While the names permitted by default are very permissive, if you feel like you have a legitimate name that wasn't matched automatically, feel free to open an issue and we can discuss adding your variable name to the list of standard ones.
