# Zuko

This is a Clojure/ClojureScript library that works with other Threatgrid data applications.

[![Clojars Project](http://clojars.org/org.clojars.quoll/zuko/latest-version.svg)](http://clojars.org/org.clojars.quoll/zuko)

The initial applications using this library are
[Naga](https://github.com/threatgrid/naga) and [Asami](https://github.com/threatgrid/asami).

This library provides functionality for:
- Managing data (especially JSON data) in a graph database. Asami describes this in the [wiki entry for Entity Structures](https://github.com/threatgrid/asami/wiki/Entity-Structure).
- Projecting data into tuples suitable for query results or insertion into a graph database.
- General utilities to avoid duplicating code between projects.

## Usage

Include a dependency to this library.

In Leiningen:

```clojure
[org.clojars.quoll/zuko "0.1.4"]
```

In `deps.edn`:

```clojure
{
  :deps {
    org.clojars.quoll/zuko {:mvn/version "0.1.4"}
  }
}
```


## License

Copyright © 2020 Cisco Systems

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
