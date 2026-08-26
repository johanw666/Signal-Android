/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.discoverability

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.ScreenshotPreviews

class PhoneNumberDiscoverabilityScreenScreenshotTests {
  @PreviewTest
  @ScreenshotPreviews
  @Composable
  fun PhoneNumberDiscoverabilityScreenPreview() {
    Previews.Preview {
      PhoneNumberDiscoverabilityScreen(
        state = PhoneNumberDiscoverabilityState(discoverable = true),
        onEvent = {}
      )
    }
  }
}
