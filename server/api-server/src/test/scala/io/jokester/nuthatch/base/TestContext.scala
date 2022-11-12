package io.jokester.nuthatch.base

import org.scalatest.BeforeAndAfterAll

trait TestContext {
  self: BeforeAndAfterAll =>
  protected lazy val testAppContext = TestAppContext.build()

  override def beforeAll(): Unit = {
    testAppContext.cleanDb()
  }

  override def afterAll(): Unit = {
    testAppContext.cleanDb()
    //    appContext.unsafeClose()
  }
}

class A {}

trait F extends A {}

abstract class B extends A with F
