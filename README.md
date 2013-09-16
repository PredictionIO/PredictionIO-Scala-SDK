PredictionIO Scala SDK
======================

Getting Started
---------------

At the moment, the Scala SDK is a collection of examples written in Scala
that make use of the PredictionIO Java SDK's client library. We envision
building a thin layer of DSL on top of the Java library when we have enough
resource. Contributions are always welcome! :)

Before running any example, make sure you have an app added in PredictionIO,
and have the app key handy.

To run the data import example,

    cd examples/import
    sbt pack
    target/pack/bin/scala-import <app key> <data file>

Each line of the data file corresponds to a rate action by a user on an item,
in tab-separted format, e.g.

    john  bacon   5
    dave  steak   4
    mary  fish    2

Notice ratings should be integers between 1 and 5. The MovieLens 100k Data Set
(http://www.grouplens.org/node/73) is a perfect sample data set if you do not
have your own to try.
