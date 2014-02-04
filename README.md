# CLJS watcher

A simple shell script that watches for changes to CLJS files and recompiles them for you.

## Usage

You must have ClojureScript already setup and have $CLOJURESCRIPT_HOME correctly set. 

Put `cljs-watch` on your $PATH (such as in /usr/local/bin) and then simply run it from your project root:

```bash
#without options it watches the src/ directory
cljs-watch

#it can also take a directory and compile options
cljs-watch -s cljs-src/ '{:optimizations :simple :pretty-print true :output-to "test.js"}'

#you can enable terminal bell ringing or even a custom command upon completed compile
cljs-watch -b -s cljs-src/
cljs-watch -c 'growlnotify -m compile-done cljs-watch' -s cljs-src/
```

## Notes
* the default output-to is set to `resources/public/cljs/bootstrap.js`
* it will add the local `lib/` to your classpath when you run it, allowing you to have other cljs deps in that folder
* to add custom macros, you can use create a folder called `cljs-macros/` from the root directory and add your macros there. You can also put macros in `CLOJURESCRIPT_HOME/lib/` to have them globally available.
* you can also place macros somewhere in your classpath and make sure the CLASSPATH environment variable is set and exported accordingly

* use space with care in the bell command, system exec does not handle it well

## License

Copyright (C) 2011 Chris Granger

Distributed under the Eclipse Public License, the same as Clojure.
