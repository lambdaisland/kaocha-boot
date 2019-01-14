Feature: Kaocha boot task

  The `kaocha` boot task provides the equivalent of invoking
  `kaocha.runner/-main` from the command line. It mimics the same behavior and
  command line options, to the extent that boot allows.

  Background:
    Given I have kaocha-boot installed as "0.0-TEST"
    And a file named "build.boot" with:
    """clojure
    (set-env! :dependencies '[[lambdaisland/kaocha-boot "0.0-TEST"]])

    (require '[kaocha.boot-task :refer [kaocha]])
    """
    And a file named "boot.properties" with:
    """
    BOOT_CLOJURE_NAME=org.clojure/clojure
    BOOT_CLOJURE_VERSION=1.10.0
    BOOT_VERSION=2.8.2
    """

  Scenario: Running tests with boot
    Given a file named "test/sample/sample_test.clj" with:
    """clojure
    (ns sample.sample-test
      (:require [clojure.test :refer :all]))

    (deftest example
      (is (= 1 1)))
    """
    When I run `boot kaocha`
    Then the output should contain:
    """
    [(.)]
    1 tests, 1 assertions, 0 failures.
    """

  Scenario: Getting Kaocha's help message
    When I run `boot kaocha --test-help`
    Then the output should contain:
    """
    USAGE:

    clj -m kaocha.runner [OPTIONS]... [TEST-SUITE]...

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
    """
