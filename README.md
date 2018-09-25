RTP Email Library - Scala library to work with Emails
==============================================================

Application built with the following (main) technologies:

- Scala
- SBT

Build and Deploy
----------------
The project is built with SBT. On a Mac (sorry everyone else) do:
```bash
brew install sbt
```

It is also a good idea to install Typesafe Activator (which sits on top of SBT) for when you need to create new projects - it also has some SBT extras, so running an application with Activator instead of SBT can be useful. On Mac do:
```bash
brew install typesafe-activator
```

To compile:
```bash
sbt compile
```

or
```bash
activator compile
```

To run the specs:
```bash
sbt test
```

The following packages up this library - Note that "assembly" will first compile and test:
```bash
sbt assembly
```
