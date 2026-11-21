package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.view.MenuItem;

import org.signal.locale.AppLocale;

import org.thoughtcrime.securesms.util.DynamicTheme;


public class ImportExportActivity extends PassphraseRequiredActivity {

  @SuppressWarnings("unused")
  private static final String TAG = ImportExportActivity.class.getSimpleName();

  private DynamicTheme dynamicTheme = new DynamicTheme();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    assert getSupportActionBar() != null;
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    initFragment(android.R.id.content, new ImportExportFragment(), AppLocale.getResolvedLocale());
  }

  @Override
  public void onResume() {
    dynamicTheme.onResume(this);
    super.onResume();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home:  finish();  return true;
    }

    return false;
  }
}
