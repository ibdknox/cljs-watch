# CLJS watcher

A simple shell script that watched for changes to CLJS files and recompiles them for you.

## Usage

Put `cljs-watch` on your $PATH (such as in /usr/local/bin) and then simply run it from a terminal:

```bash
cljs-watch

#it can also take a directory and compile options
cljs-watch cljs-src/ {:optimizations :none}
```

## Notes
* by default output-to is set to `resources/public/cljs/bootstrap.js`
* it will add the local `lib/` to your classpath when you run it, allowing you to have other cljs deps in that folder

## License

Copyright (C) 2011 Chris Granger

Distributed under the Eclipse Public License, the same as Clojure.
