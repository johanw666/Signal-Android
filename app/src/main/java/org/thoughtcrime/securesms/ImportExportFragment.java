package org.thoughtcrime.securesms;

import android.Manifest;
import androidx.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.backup.BackupDialog;
import org.thoughtcrime.securesms.conversationlist.ConversationListFragment;
import org.thoughtcrime.securesms.database.EncryptedBackupExporter;
import org.thoughtcrime.securesms.database.NoExternalStorageException;
import org.thoughtcrime.securesms.database.PlaintextBackupExporter;
import org.thoughtcrime.securesms.database.PlaintextBackupImporter;
import org.thoughtcrime.securesms.database.WhatsappBackupImporter;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.service.ApplicationMigrationService;
import org.thoughtcrime.securesms.util.UriUtils;

import java.io.File;
import java.io.IOException;


public class ImportExportFragment extends Fragment {

  private static final String TAG = ImportExportFragment.class.getSimpleName();

  private static final int SUCCESS    = 0;
  private static final int NO_SD_CARD = 1;
  private static final int ERROR_IO   = 2;
  private static final short CHOOSE_PLAINTEXT_IMPORT_LOCATION_REQUEST_CODE = 3;
  private static final short CHOOSE_PLAINTEXT_EXPORT_LOCATION_REQUEST_CODE = 4;
  private static final short CHOOSE_FULL_IMPORT_LOCATION_REQUEST_CODE = 5;
  private static final short CHOOSE_FULL_EXPORT_LOCATION_REQUEST_CODE = 6;

  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    View layout              = inflater.inflate(R.layout.import_export_fragment, container, false);
    View importSmsView       = layout.findViewById(R.id.import_sms             );
    View importWhatsappView  = layout.findViewById(R.id.import_whatsapp_backup);
    View importPlaintextView = layout.findViewById(R.id.import_plaintext_backup);
    View importEncryptedView = layout.findViewById(R.id.import_encrypted_backup);
    View exportPlaintextView = layout.findViewById(R.id.export_plaintext_backup);
    View exportEncryptedView = layout.findViewById(R.id.export_encrypted_backup);

    importSmsView.setOnClickListener(v -> handleImportSms());
    importWhatsappView.setOnClickListener(v -> handleImportWhatsappBackup());
    importPlaintextView.setOnClickListener(v -> handleImportPlaintextBackup());
    importEncryptedView.setOnClickListener(v -> handleImportEncryptedBackup());
    exportPlaintextView.setOnClickListener(v -> handleExportPlaintextBackup());
    exportEncryptedView.setOnClickListener(v -> handleExportEncryptedBackup());

    return layout;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    if (progressDialog != null && progressDialog.isShowing()) {
      progressDialog.dismiss();
      progressDialog = null;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @SuppressWarnings("CodeBlock2Expr")
  private void handleImportSms() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setIcon(R.drawable.ic_info_outline);
    builder.setTitle(getActivity().getString(R.string.ImportFragment_import_system_sms_database));
    builder.setMessage(getActivity().getString(R.string.ImportFragment_this_will_import_messages_from_the_system));
    builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_import), (dialog, which) -> {
      Permissions.with(this)
                 .request(Manifest.permission.READ_SMS)
                 .ifNecessary()
                 .withPermanentDenialDialog(getString(R.string.ImportExportFragment_signal_needs_the_sms_permission_in_order_to_import_sms_messages))
                 .onAllGranted(() -> {
                   Intent intent = new Intent(getActivity(), ApplicationMigrationService.class);
                   intent.setAction(ApplicationMigrationService.MIGRATE_DATABASE);
                   getActivity().startService(intent);

                   Intent nextIntent = new Intent(getActivity(), ConversationListFragment.class);

                   Intent activityIntent = new Intent(getActivity(), DatabaseMigrationActivity.class);
                   activityIntent.putExtra("next_intent", nextIntent);
                   getActivity().startActivity(activityIntent);
                 })
                 .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_signal_needs_the_sms_permission_in_order_to_import_sms_messages_toast, Toast.LENGTH_LONG).show())
                 .execute();
    });
    builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
    builder.show();
  }

  @SuppressWarnings("CodeBlock2Expr")
  @SuppressLint("InlinedApi")
  private void handleImportWhatsappBackup() {
    CheckAndGetAccessPermissionApi30();
    if (existsWhatsAppMessageDatabase()) {
      AlertDialog.Builder builder = ImportWhatsappDialog.getWhatsappBackupDialog(getActivity());
      builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_import), (dialog, which) -> {
        Permissions.with(ImportExportFragment.this)
                   .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                   .ifNecessary()
                   .withPermanentDenialDialog(getString(R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_read_from_external_storage_but_it_has_been_permanently_denied))
                   .onAllGranted(() -> new ImportWhatsappBackupTask(ImportWhatsappDialog.isImportGroups(), ImportWhatsappDialog.isAvoidDuplicates(), ImportWhatsappDialog.isImportMedia()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                   .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_read_from_external_storage, Toast.LENGTH_LONG).show())
                   .execute();
      });
      builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
      builder.show();
    } else {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(getActivity().getString(R.string.ImportFragment_no_whatsapp_backup_found))
             .setCancelable(false)
             .setPositiveButton(getActivity().getString(R.string.ImportFragment_restore_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              });
      AlertDialog alert = builder.create();
      alert.show();
    }
  }

  @SuppressLint("StaticFieldLeak")
  public class ImportWhatsappBackupTask extends AsyncTask<Void, Void, Integer> {

    private final boolean importGroups;
    private final boolean importMedia;
    private final boolean avoidDuplicates;

    public ImportWhatsappBackupTask(boolean importGroups, boolean avoidDuplicates, boolean importMedia) {
      this.importGroups = importGroups;
      this.avoidDuplicates = avoidDuplicates;
      this.importMedia = importMedia;
    }

    @Override
    protected void onPreExecute() {
      progressDialog = new ProgressDialog(getActivity());
      progressDialog.setTitle(getActivity().getString(R.string.ImportFragment_importing));
      progressDialog.setMessage(getActivity().getString(R.string.ImportFragment_import_whatsapp_backup_elipse));
      progressDialog.setCancelable(false);
      progressDialog.setIndeterminate(false);
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progressDialog.show();
    }

    protected void onPostExecute(Integer result) {
      Context context = getActivity();

      if (progressDialog != null)
        progressDialog.dismiss();

      if (context == null)
        return;

      switch (result) {
        case NO_SD_CARD:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_no_whatsapp_backup_found),
                         Toast.LENGTH_LONG).show();
          break;
        case ERROR_IO:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_error_importing_backup),
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_import_complete),
                         Toast.LENGTH_LONG).show();
          break;
      }
    }

    @Override
    protected Integer doInBackground(Void... params) {
      try {
        WhatsappBackupImporter.importWhatsappFromSd(getActivity(), progressDialog, importGroups, avoidDuplicates, importMedia);
        return SUCCESS;
      } catch (NoExternalStorageException e) {
        Log.w(TAG, e);
        return NO_SD_CARD;
      } catch (IOException e) {
        Log.w(TAG, e);
        return ERROR_IO;
      }
    }
  }

  @SuppressWarnings("CodeBlock2Expr")
  @SuppressLint("InlinedApi")
  private void handleImportPlaintextBackup() {
    CheckAndGetAccessPermissionApi30();
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setIcon(R.drawable.ic_warning);
    builder.setTitle(getActivity().getString(R.string.ImportFragment_import_plaintext_backup));
    builder.setMessage(getActivity().getString(R.string.ImportFragment_this_will_import_messages_from_a_plaintext_backup));
    builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_import), (dialog, which) -> {
      if (Build.VERSION.SDK_INT >= 30 && SignalStore.settings().getSignalBackupDirectory() == null ) {
        BackupDialog.showChooseBackupLocationDialog(this, CHOOSE_PLAINTEXT_IMPORT_LOCATION_REQUEST_CODE);
      } else if (Build.VERSION.SDK_INT < 29) {
        Permissions.with(ImportExportFragment.this)
                   .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                   .ifNecessary()
                   .withPermanentDenialDialog(getString(R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_read_from_external_storage_but_it_has_been_permanently_denied))
                   .onAllGranted(() ->
                                     new ImportPlaintextBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                   .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_read_from_external_storage, Toast.LENGTH_LONG).show())
                   .execute();
      } else {
        new ImportPlaintextBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });
    builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
    builder.show();
  }

  @SuppressWarnings("CodeBlock2Expr")
  @SuppressLint("InlinedApi")
  private void handleExportPlaintextBackup() {
    CheckAndGetAccessPermissionApi30();
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setIcon(R.drawable.ic_warning);
    builder.setTitle(getActivity().getString(R.string.ExportFragment_export_plaintext_to_storage));
    builder.setMessage(getActivity().getString(R.string.ExportFragment_warning_this_will_export_the_plaintext_contents));
    builder.setPositiveButton(getActivity().getString(R.string.ExportFragment_export), (dialog, which) -> {
      if (Build.VERSION.SDK_INT >= 30 && SignalStore.settings().getSignalBackupDirectory() == null ) {
        BackupDialog.showChooseBackupLocationDialog(this, CHOOSE_PLAINTEXT_EXPORT_LOCATION_REQUEST_CODE);
      } else if (Build.VERSION.SDK_INT < 29) {
        Permissions.with(ImportExportFragment.this)
                   .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                   .ifNecessary()
                   .withPermanentDenialDialog(getString(R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
                   .onAllGranted(() -> new ExportPlaintextTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                   .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_write_to_external_storage, Toast.LENGTH_LONG).show())
                   .execute();
      } else {
        new ExportPlaintextTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });
    builder.setNegativeButton(getActivity().getString(R.string.ExportFragment_cancel), null);
    builder.show();
  }

  @SuppressLint("StaticFieldLeak")
  private class ImportPlaintextBackupTask extends AsyncTask<Void, Void, Integer> {

    @Override
    protected void onPreExecute() {
      progressDialog = ProgressDialog.show(getActivity(),
                                           getActivity().getString(R.string.ImportFragment_importing),
                                           getActivity().getString(R.string.ImportFragment_import_plaintext_backup_elipse),
                                           true, false);
    }

    protected void onPostExecute(Integer result) {
      Context context = getActivity();

      if (progressDialog != null)
        progressDialog.dismiss();

      if (context == null)
        return;

      switch (result) {
        case NO_SD_CARD:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_no_plaintext_backup_found),
                         Toast.LENGTH_LONG).show();
          break;
        case ERROR_IO:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_error_importing_backup),
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_import_complete),
                         Toast.LENGTH_LONG).show();
          break;
      }
    }

    @Override
    protected Integer doInBackground(Void... params) {
      try {
        PlaintextBackupImporter.importPlaintextFromSd(getActivity());
        return SUCCESS;
      } catch (NoExternalStorageException e) {
        Log.w(TAG, e);
        return NO_SD_CARD;
      } catch (IOException e) {
        Log.w(TAG, e);
        return ERROR_IO;
      }
    }
  }

  @SuppressLint("StaticFieldLeak")
  private class ExportPlaintextTask extends AsyncTask<Void, Void, Integer> {
    private ProgressDialog dialog;

    @Override
    protected void onPreExecute() {
      dialog = ProgressDialog.show(getActivity(),
                                   getActivity().getString(R.string.ExportFragment_exporting),
                                   getActivity().getString(R.string.ExportFragment_exporting_plaintext_to_storage),
                                   true, false);
    }

    @Override
    protected Integer doInBackground(Void... params) {
      try {
        PlaintextBackupExporter.exportPlaintextToSd(getActivity());
        return SUCCESS;
      } catch (NoExternalStorageException e) {
        Log.w(TAG, e);
        return NO_SD_CARD;
      } catch (IOException e) {
        Log.w(TAG, e);
        return ERROR_IO;
      }
    }

    @Override
    protected void onPostExecute(Integer result) {
      Context context = getActivity();

      if (dialog != null)
        dialog.dismiss();

      if (context == null)
        return;

      switch (result) {
        case NO_SD_CARD:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_error_unable_to_write_to_storage),
                         Toast.LENGTH_LONG).show();
          break;
        case ERROR_IO:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_error_while_writing_to_storage),
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_export_successful),
                         Toast.LENGTH_LONG).show();
          break;
      }
    }
  }

  public void handleImportEncryptedBackup() {
    CheckAndGetAccessPermissionApi30();
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setIcon(R.drawable.ic_warning);
    builder.setTitle(getActivity().getString(R.string.ImportFragment_restore_encrypted_backup));
    builder.setMessage(getActivity().getString(R.string.ImportFragment_restoring_an_encrypted_backup_will_completely_replace_your_existing_keys));
    builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_restore), (dialog, which) -> {
      if (Build.VERSION.SDK_INT >= 30 && SignalStore.settings().getSignalBackupDirectory() == null ) {
        BackupDialog.showChooseBackupLocationDialog(this, CHOOSE_FULL_IMPORT_LOCATION_REQUEST_CODE);
      } else if (Build.VERSION.SDK_INT < 29) {
        Permissions.with(ImportExportFragment.this)
                   .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                   .ifNecessary()
                   .withPermanentDenialDialog(getString(R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_read_from_external_storage_but_it_has_been_permanently_denied))
                   .onAllGranted(() -> new ImportEncryptedBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                   .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_read_from_external_storage, Toast.LENGTH_LONG).show())
                   .execute();
      } else {
        new ImportEncryptedBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });
    builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
    builder.show();
  }

  @SuppressLint("StaticFieldLeak")
  private class ImportEncryptedBackupTask extends AsyncTask<Void, Void, Integer> {

    @Override
    protected void onPreExecute() {
      progressDialog = ProgressDialog.show(getActivity(),
                                           getActivity().getString(R.string.ImportFragment_restoring),
                                           getActivity().getString(R.string.ImportFragment_restoring_encrypted_backup),
                                           true, false);
    }

    protected void onPostExecute(Integer result) {
      Context context = getActivity();

      if (progressDialog != null)
        progressDialog.dismiss();

      if (context == null)
        return;

      switch (result) {
        case NO_SD_CARD:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_no_encrypted_backup_found),
                         Toast.LENGTH_LONG).show();
          break;
        case ERROR_IO:
          Toast.makeText(context,
                         context.getString(R.string.ImportFragment_error_importing_backup),
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          //DatabaseFactory.getInstance(context).reset(context);
          //Intent intent = new Intent(context, KeyCachingService.class);
          //intent.setAction(KeyCachingService.CLEAR_KEY_ACTION);
          //context.startService(intent);

          // JW: Restart after OK press
          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
          builder.setMessage(context.getString(R.string.ImportFragment_restore_complete))
                  .setCancelable(false)
                  .setPositiveButton(context.getString(R.string.ImportFragment_restore_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                      ExitActivity.exitAndRemoveFromRecentApps(getActivity());
                    }
                  });
          AlertDialog alert = builder.create();
          alert.show();
      }
    }

    @Override
    protected Integer doInBackground(Void... params) {
      try {
        EncryptedBackupExporter.importFromSd(getActivity());
        return SUCCESS;
      } catch (NoExternalStorageException e) {
        Log.w(TAG, e);
        return NO_SD_CARD;
      } catch (IOException e) {
        Log.w(TAG, e);
        return ERROR_IO;
      }
    }
  }

  private void handleExportEncryptedBackup() {
    CheckAndGetAccessPermissionApi30();
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setIcon(R.drawable.ic_info_outline);
    builder.setTitle(getActivity().getString(R.string.ExportFragment_export_to_sd_card));
    builder.setMessage(getActivity().getString(R.string.ExportFragment_this_will_export_your_encrypted_keys_settings_and_messages));
    builder.setPositiveButton(getActivity().getString(R.string.ExportFragment_export), (dialog, which) -> {
      if (Build.VERSION.SDK_INT >= 30 && SignalStore.settings().getSignalBackupDirectory() == null ) {
        BackupDialog.showChooseBackupLocationDialog(this, CHOOSE_FULL_EXPORT_LOCATION_REQUEST_CODE);
      } else if (Build.VERSION.SDK_INT < 29) {
        Permissions.with(ImportExportFragment.this)
                   .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                   .ifNecessary()
                   .withPermanentDenialDialog(getString(R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
                   .onAllGranted(() -> new ExportEncryptedTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                   .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_signal_needs_the_storage_permission_in_order_to_write_to_external_storage, Toast.LENGTH_LONG).show())
                   .execute();
      } else {
        new ExportEncryptedTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });
    builder.setNegativeButton(getActivity().getString(R.string.ExportFragment_cancel), null);
    builder.show();
  }

  private class ExportEncryptedTask extends AsyncTask<Void, Void, Integer> {
    private ProgressDialog dialog;

    @Override
    protected void onPreExecute() {
      dialog = ProgressDialog.show(getActivity(),
                                   getActivity().getString(R.string.ExportFragment_exporting),
                                   getActivity().getString(R.string.ExportFragment_exporting_keys_settings_and_messages),
                                   true, false);
    }

    @Override
    protected void onPostExecute(Integer result) {
      Context context = getActivity();

      if (dialog != null) dialog.dismiss();

      if (context == null) return;

      switch (result) {
        case NO_SD_CARD:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_error_unable_to_write_to_storage),
                         Toast.LENGTH_LONG).show();
          break;
        case ERROR_IO:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_error_while_writing_to_storage),
                         Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          Toast.makeText(context,
                         context.getString(R.string.ExportFragment_export_successful),
                         Toast.LENGTH_LONG).show();
          break;
      }
    }

    @Override
    protected Integer doInBackground(Void... params) {
      try {
        EncryptedBackupExporter.exportToSd(getActivity());
        return SUCCESS;
      } catch (NoExternalStorageException e) {
        Log.w(TAG, e);
        return NO_SD_CARD;
      } catch (IOException e) {
        Log.w(TAG, e);
        return ERROR_IO;
      }
    }
  }

  private boolean existsWhatsAppMessageDatabase() {
    String dbfile = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "msgstore.db";
    File msgdb = new File(dbfile);
    if (msgdb.exists()) return true; else return false;
  }

  private void CheckAndGetAccessPermissionApi30() {
    if (Build.VERSION.SDK_INT >= 30) {
      if (!Environment.isExternalStorageManager()) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getActivity().getString(R.string.ImportExportFragment_signal_needs_the_all_files_access_permission))
               .setCancelable(false)
               .setPositiveButton(getActivity().getString(R.string.ImportFragment_restore_ok), new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                   Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                   startActivity(intent);
                 }
               });
        AlertDialog alert = builder.create();
        alert.show();
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (data != null && data.getData() != null) {
      Uri backupDirectoryUri = data.getData();
      SignalStore.settings().setSignalBackupDirectory(backupDirectoryUri);
      String backupDir = UriUtils.getFullPathFromTreeUri(getActivity(), backupDirectoryUri);
      int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

      Log.w(TAG, "Take permission from Uri: " + backupDir);
      getActivity().getContentResolver().takePersistableUriPermission(backupDirectoryUri, takeFlags);

      if (resultCode == Activity.RESULT_OK) {
        switch (requestCode) {
          case CHOOSE_PLAINTEXT_IMPORT_LOCATION_REQUEST_CODE:
            new ImportPlaintextBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            break;
          case CHOOSE_PLAINTEXT_EXPORT_LOCATION_REQUEST_CODE:
            new ExportPlaintextTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            break;
          case CHOOSE_FULL_IMPORT_LOCATION_REQUEST_CODE:
            new ImportEncryptedBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            break;
          case CHOOSE_FULL_EXPORT_LOCATION_REQUEST_CODE:
            new ExportEncryptedTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            break;
        }
      }
    }
  }

}

