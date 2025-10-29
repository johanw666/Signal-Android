package org.thoughtcrime.securesms.components.settings.app.privacy

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.thoughtcrime.securesms.util.livedata.Store

class PrivacySettingsViewModel(
  private val sharedPreferences: SharedPreferences,
  private val repository: PrivacySettingsRepository
) : ViewModel() {

  private val store = Store(getState())

  val state: LiveData<PrivacySettingsState> = store.stateLiveData

  fun refreshBlockedCount() {
    repository.getBlockedCount { count ->
      store.update { it.copy(blockedCount = count) }
      refresh()
    }
  }

  fun setReadReceiptsEnabled(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(TextSecurePreferences.READ_RECEIPTS_PREF, enabled).apply()
    repository.syncReadReceiptState()
    refresh()
  }

  fun setTypingIndicatorsEnabled(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(TextSecurePreferences.TYPING_INDICATORS, enabled).apply()
    repository.syncTypingIndicatorsState()
    refresh()
  }

  fun setScreenSecurityEnabled(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(TextSecurePreferences.SCREEN_SECURITY_PREF, enabled).apply()
    refresh()
  }

  fun setIncognitoKeyboard(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(TextSecurePreferences.INCOGNITO_KEYBORAD_PREF, enabled).apply()
    refresh()
  }

  fun togglePaymentLock(enable: Boolean) {
    SignalStore.payments.paymentLock = enable
    refresh()
  }

  fun setObsoletePasswordTimeoutEnabled(enabled: Boolean) {
    SignalStore.settings.passphraseTimeoutEnabled = enabled
    refresh()
  }

  fun setObsoletePasswordTimeout(minutes: Int) {
    SignalStore.settings.passphraseTimeout = minutes
    refresh()
  }

  // JW: added
  fun setPassphraseEnabled(enabled: Boolean) {
    SignalStore.settings.setPassphraseDisabled(!enabled)
    SignalStore.settings.setScreenLockEnabled(!enabled)
    sharedPreferences.edit().putBoolean("pref_enable_passphrase_temporary", enabled).apply()
    refresh()
  }

  // JW: added
  fun setOnlyScreenlockEnabled(enabled: Boolean) {
    SignalStore.settings.setPassphraseDisabled(true)
    SignalStore.settings.setScreenLockEnabled(enabled)
    sharedPreferences.edit().putBoolean("pref_enable_passphrase_temporary", false).apply()
    refresh()
  }

  // JW: added
  fun setNoLock() {
    SignalStore.settings.setPassphraseDisabled(true)
    SignalStore.settings.setScreenLockEnabled(false)
    sharedPreferences.edit().putBoolean("pref_enable_passphrase_temporary", false).apply()
    refresh()
  }

  // JW: added method.
  fun isPassphraseSelected(): Boolean {
    // Because this preference may be undefined when this app is first ran we also check if there is a passphrase
    // defined, if so, we assume passphrase protection:
    val myContext = AppDependencies.application

    return TextSecurePreferences.isProtectionMethodPassphrase(myContext) ||
      TextSecurePreferences.getBooleanPreference(myContext, "pref_enable_passphrase_temporary", false) && !SignalStore.settings.getPassphraseDisabled()
  }

  fun refresh() {
    store.update(this::updateState)
  }

  private fun getState(): PrivacySettingsState {
    return PrivacySettingsState(
      blockedCount = 0,
      readReceipts = TextSecurePreferences.isReadReceiptsEnabled(AppDependencies.application),
      typingIndicators = TextSecurePreferences.isTypingIndicatorsEnabled(AppDependencies.application),
      screenLock = SignalStore.settings.screenLockEnabled,
      screenLockActivityTimeout = SignalStore.settings.screenLockTimeout,
      screenSecurity = TextSecurePreferences.isScreenSecurityEnabled(AppDependencies.application),
      incognitoKeyboard = TextSecurePreferences.isIncognitoKeyboardEnabled(AppDependencies.application),
      paymentLock = SignalStore.payments.paymentLock,
      isObsoletePasswordEnabled = !SignalStore.settings.passphraseDisabled,
      isObsoletePasswordTimeoutEnabled = SignalStore.settings.passphraseTimeoutEnabled,
      obsoletePasswordTimeout = SignalStore.settings.passphraseTimeout,
      universalExpireTimer = SignalStore.settings.universalExpireTimer
      // JW: added
      ,
      isProtectionMethodPassphrase = TextSecurePreferences.isProtectionMethodPassphrase(AppDependencies.application)
    )
  }

  private fun updateState(state: PrivacySettingsState): PrivacySettingsState {
    return getState().copy(blockedCount = state.blockedCount)
  }

  class Factory(
    private val sharedPreferences: SharedPreferences,
    private val repository: PrivacySettingsRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(PrivacySettingsViewModel(sharedPreferences, repository)))
    }
  }
}
