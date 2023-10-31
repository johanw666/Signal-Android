package org.thoughtcrime.securesms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.app.AlertDialog;

public class ImportWhatsappDialog {

    private static boolean importGroups = false;
    private static boolean avoidDuplicates = false;
    private static boolean importMedia = false;

    @SuppressWarnings("CodeBlock2Expr")
    @SuppressLint("InlinedApi")
    public static AlertDialog.Builder getWhatsappBackupDialog(Activity activity) {
        View checkBoxView = View.inflate(activity, R.layout.dialog_import_whatsapp, null);
        CheckBox importGroupsCheckbox = checkBoxView.findViewById(R.id.import_groups_checkbox);
        importGroupsCheckbox.setChecked(importGroups);
        importGroupsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ImportWhatsappDialog.importGroups = isChecked;
        });

        CheckBox avoidDuplicatesCheckbox = checkBoxView.findViewById(R.id.avoid_duplicates_checkbox);
        avoidDuplicatesCheckbox.setChecked(avoidDuplicates);
        avoidDuplicatesCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ImportWhatsappDialog.avoidDuplicates = isChecked;
        });

        CheckBox importMediaCheckbox = checkBoxView.findViewById(R.id.import_media_checkbox);
        importMediaCheckbox.setChecked(importMedia);
        importMediaCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ImportWhatsappDialog.importMedia = isChecked;
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.drawable.ic_warning);
        builder.setTitle(activity.getString(R.string.ImportFragment_import_whatsapp_backup));
        builder.setMessage(activity.getString(R.string.ImportFragment_this_will_import_messages_from_whatsapp_backup))
                .setView(checkBoxView);
        return builder;
    }

    public static boolean isImportGroups() {
        return importGroups;
    }

    public static boolean isAvoidDuplicates() {
        return avoidDuplicates;
    }

    public static boolean isImportMedia() {
        return importMedia;
    }
}
