package dev.capslock.exercise.ox

import ox.*
import ox.channels.*
import ox.retry.*

import scala.concurrent.duration.*
import scala.util.Try

// Caveat! This code requires Project Loom; Use JDK 21 or later
object Main:
  @main def hello(): Unit =
    println("Hello world!")
    println("Going to use Ox")
    race(
      () => { sleep(1.second); println("Hello") },
      () => { sleep(2.second); println("World") },
    )()
    par(
      () => {
        println("Starting first fiber")
        sleep(1.second)
        println("First done")
      },
      () => {
        println("Starting second fiber")
        sleep(2.second)
        println("Second done")
      },
    )
    join
    controlingTimeout
    supervisedExecution
    retryingExecution
    transformation
    channels

  def join =
    // use def instead of val to avoid eager evaluation
    def heavyComputation1 = {
      println("starting heavy computation 1")
      sleep(2.second)
      println("heavy computation 1 done")
      42
    }
    def heavyComputation2 = {
      println("starting heavy computation 2")
      sleep(1.second)
      println("heavy computation 2 done")
      43
    }
    println("waiting for the result")
    val tupled = par(heavyComputation1, heavyComputation2)
    println(tupled)

  def controlingTimeout =
    def computation = {
      println("starting heavy computation")
      sleep(2.second)
      println("heavy computation done")
      42
    }
    val result = Try(timeout(1.second)(computation))
    println(result)

  def supervisedExecution =
    Try(supervised {
      println("Starting supervised execution")
      forkUser {
        sleep(1.second)
        // This won't be executed due to the exception
        // This will be automatically cancelled
        println("Hello!")
      }
      forkUser {
        sleep(500.millis)
        throw new RuntimeException("boom!")
      }
    })

  def retryingExecution =
    def randomlyFail = {
      if (math.random() < 0.8) {
        println("Failed")
        throw new RuntimeException("boom!")
      }
      println("Succeeded")
      42
    }
    Try(
      ox.retry.retry(
        RetryPolicy.backoff(
          maxRetries = 10,
          initialDelay = 100.millis,
          maxDelay = 1.second,
          jitter = Jitter.Equal,
        ),
      )(randomlyFail),
    )

  def transformation =
    supervised {
      Source
        .iterate(0)(_ + 1) // natural number
        .filter(_ % 2 == 0) // even number
        .mapParUnordered(4) { n =>
          sleep((Math.random * 100).millis)
          n + 1
        // add 1
        }
        .take(100)
        .foreach(n => println(n.toString))
    }

  def channels =
    val chan1 = Channel.rendezvous[String]
    val chan2 = Channel.rendezvous[String]
    supervised {
      forkUser {
        // chan requires 1 second to process each message
        chan1.foreach { s =>
          sleep(1.second)
          println(s)
        }
      }
      forkUser {
        chan2.map(_.toUpperCase).foreach { s =>
          sleep(1.second)
          println(s)
        }
      }
      forkUser {
        // send message to either chan1 or chan2
        select(chan1.sendClause("Hello"), chan2.sendClause("Hello"))
        sleep(0.5.second)
        // earlier channel is still blocking
        select(chan1.sendClause("World"), chan2.sendClause("World"))
        sleep(0.5.second)
        chan1.done()
        chan2.done()
      }
    }
