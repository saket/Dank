package me.saket.dank

import io.reactivex.plugins.RxJavaPlugins
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.LinkedBlockingDeque

/**
 * Starting from RxJava2, uncaught errors are not thrown but instead sent to
 * the thread's default error handler. This results in a situation where JUnit
 * would always pass even if any RxJava chain encountered an error.
 *
 * More info about this issue:
 * https://github.com/ReactiveX/RxJava/issues/5234.
 *
 * This rule was copied from AutoDispose:
 * https://github.com/uber/AutoDispose/blob/master/test-utils/src/main/java/com/uber/autodispose/test/RxErrorsRule.java
 */
class RxErrorsRule : TestRule {

  private val errors = LinkedBlockingDeque<Throwable>()

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        RxJavaPlugins.setErrorHandler { t -> errors.add(t) }

        try {
          base.evaluate()

        } finally {
          RxJavaPlugins.setErrorHandler(null)
          assertNoErrors()
        }
      }
    }
  }

  private fun hasErrors(): Boolean {
    val error = errors.peek()
    return error != null
  }

  fun assertNoErrors() {
    if (hasErrors()) {
      errors.forEach {
        it.printStackTrace()
      }
      throw AssertionError("Expected no errors but found $errors")
    }
  }
}
