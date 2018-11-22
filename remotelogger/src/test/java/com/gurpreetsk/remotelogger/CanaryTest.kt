package com.gurpreetsk.remotelogger

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanaryTest {
  @Test fun `This is the project's canary test, and should fail if unit test framework setup is not correct`() {
    assertThat(true)
        .isEqualTo(true)
  }
}
