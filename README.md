# kaocha-boot

<!-- badges -->
[![CircleCI](https://circleci.com/gh/lambdaisland/kaocha-boot.svg?style=svg)](https://circleci.com/gh/lambdaisland/kaocha-boot) [![cljdoc badge](https://cljdoc.org/badge/lambdaisland/kaocha-boot)](https://cljdoc.org/d/lambdaisland/kaocha-boot) [![Clojars Project](https://img.shields.io/clojars/v/lambdaisland/kaocha-boot.svg)](https://clojars.org/lambdaisland/kaocha-boot) [![codecov](https://codecov.io/gh/lambdaisland/kaocha-boot/branch/master/graph/badge.svg)](https://codecov.io/gh/lambdaisland/kaocha-boot)
<!-- /badges -->

Boot support for Kaocha.

## Installation

In your `build.boot` add the Kaocha dependency, and import the Kaocha task

``` clojure
;; build.boot
(set-env! :source-paths #{"src"}
          :dependencies '[[lambdaisland/kaocha-boot "0.0-14"]])

(require '[kaocha.boot-task :refer [kaocha]])
```

Configure your test suites in `tests.edn`, see the
[Kaocha](http://github.com/lambdaisland/kaocha) documentation for `tests.edn`
syntax.

``` clojure
;; tests.edn
#kaocha/v1
{}
```

As with Leiningen and Clojure CLI we still recommend creating a `bin/kaocha`
wrapper script, so that Kaocha can be invoked uniformly across projects,
regardless of the tool used.

``` bash
#!/bin/bash

boot kaocha "$@"
```

## Running Kaocha

You invoke the Kaocha task with `boot kaocha`. This tasks takes the same command
line argument as Leiningen or Clojure CLI runner, with some caveats.

To only run specific test suite, use the `-s` / `--suite` option, rather than
providing them directly as command line arguments.


``` bash
boot kaocha --suite unit

# Clojure CLI / Leiningen equivalent
# bin/kaocha unit
```

Kaocha's plugin system allows plugins to register extra command line options.
The static nature of Boot's `deftask` construct does not however allow for
runtime registration of command line options. To work around this an extra
`--options` flag is provided which takes an EDN map of additional options.

For example:

``` bash
boot kaocha --options '{:skip-meta [:slow :win32]}'

# Clojure CLI / Leiningen equivalent:
# bin/kaocha --skip-meta :slow --skip-meta :win32
```

The `--test-help` flag will print all available options and exit, including the
options that are recognized in the `--options {}` EDN map. Note that this
depends on the plugins enabled in `tests.edn` or through the `--plugin` option.
The version shown here only includes those options provided by built-in plugins
that are enabled by default.

Note that `--help` will only print the options boot know's about. For complete
usage information always use `--H` / `--test-help`.

```
$ boot kaocha --test-help

USAGE:

boot kaocha [OPTIONS]...

  -s, --suite SUITE                  Test suite(s) to run.
  -c, --config-file FILE  tests.edn  Config file to read.
      --print-config                 Print out the fully merged and normalized config, then exit.
      --print-test-plan              Load tests, build up a test plan, then print out the test plan and exit.
      --print-result                 Print the test result map as returned by the Kaocha API.
      --fail-fast                    Stop testing after the first failure.
      --[no-]color                   Enable/disable ANSI color codes in output. Defaults to true.
      --[no-]watch                   Watch filesystem for changes and re-run tests.
      --reporter SYMBOL              Change the test reporter, can be specified multiple times.
      --plugin KEYWORD               Load the given plugin.
      --version                      Print version information and quit.
      --help                         Display this help message.
  -H, --test-help                    Display this help message.
  -o, --options EDN                  EDN map of additional options.

Plugin-specific options can be specified using map syntax, e.g.


   boot kaocha --options '{:randomize false}'

These additional options are recognized:

   :randomize BOOL                Run test namespaces and vars in random order.
   :seed SEED                     Provide a seed to determine the random order of tests.
   :skip SYM                      Skip tests with this ID and their children.
   :focus SYM                     Only run this test, skip others.
   :skip-meta SYM                 Skip tests where this metadata key is truthy.
   :focus-meta SYM                Only run tests where this metadata key is truthy.
   :capture-output BOOL           Capture output during tests.
```

## Exit code

The Kaocha task will exit the JVM with a non-zero exit code after a run with
errors or failures. This does mean that any tasks following the Kaocha task will
only run if the build passes.

You can use this to your advantage, e.g. to only perform build steps when the
tests pass, but it does mean that Kaocha does not compose like some other tasks.
To run Kaocha continuously use Kaocha's own `--watch` functionality, rather than
Boot's.

## License

Copyright &copy; 2018 Arne Brasseur

Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt
