package com.gurpreetsk.remotelogger

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AndroidCanaryTest {
  @Test fun canaryTest_willFailIfAndroidTestingSetupIsWrong() {
    assertThat(true)
        .isTrue()
  }
}
