/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.signallogin

import org.signal.core.models.AccountEntropyPool
import org.signal.core.models.ServiceId.ACI
import org.thoughtcrime.securesms.keyvalue.SignalStore

/**
 * Where [SignalLoginViewDetailsViewModel] reads the credentials that make up the user's Signal Login.
 */
class SignalLoginViewDetailsRepository {

  fun getAci(): ACI? = SignalStore.account.aci

  fun getAccountEntropyPool(): AccountEntropyPool? = SignalStore.account.accountEntropyPoolOrNull
}
