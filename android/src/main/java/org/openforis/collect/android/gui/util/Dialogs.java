package org.openforis.collect.android.gui.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import org.openforis.collect.R;
import org.openforis.commons.collection.Predicate;

public class Dialogs {

    public static AlertDialog alert(Context context, int titleKey, int messageKey) {
        return alert(context, titleKey, messageKey, null);
    }

    public static AlertDialog alert(Context context, int titleKey, int messageKey, final Runnable runOnPositiveButtonClick) {
        return alert(context, context.getResources().getString(titleKey), context.getResources().getString(messageKey), runOnPositiveButtonClick);
    }

    public static AlertDialog alert(Context context, int titleKey, String message) {
        return alert(context, context.getResources().getString(titleKey), message, null);
    }

    public static AlertDialog alert(Context context, String title, String message) {
        return alert(context, title, message, null);
    }

    public static AlertDialog alert(Context context, String title, String message, final Runnable runOnPositiveButtonClick) {
        return showDialog(context, title, message, android.R.drawable.ic_dialog_alert, runOnPositiveButtonClick);
    }

    public static AlertDialog info(Context context, int titleKey, int messageKey) {
        return info(context, titleKey, context.getResources().getString(messageKey), null);
    }

    public static AlertDialog info(Context context, int titleKey, String message) {
        return info(context, titleKey, message, null);
    }

    public static AlertDialog info(Context context, int titleKey, String message, final Runnable runOnPositiveButtonClick) {
        return showDialog(context, context.getResources().getString(titleKey), message, android.R.drawable.ic_dialog_info, runOnPositiveButtonClick);
    }

    private static AlertDialog showDialog(Context context, String title, String message, int icon, final Runnable runOnPositiveButtonClick) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (runOnPositiveButtonClick != null) {
                            runOnPositiveButtonClick.run();
                        }
                    }
                })
                .setIcon(icon)
                .create();
        dialog.show();
        return dialog;
    }

    public static AlertDialog confirm(Context context, int titleKey, int messageKey,
                                      final Runnable runOnPositiveButtonClick) {
        return confirm(context, titleKey, messageKey, runOnPositiveButtonClick, null);
    }

    public static AlertDialog confirm(Context context, int titleKey, String message,
                                      final Runnable runOnPositiveButtonClick) {
        return confirm(context, titleKey, message, runOnPositiveButtonClick, null, R.string.confirm_label, R.string.cancel_label);
    }

    public static AlertDialog confirm(Context context, int titleKey, int messageKey,
                                      final Runnable runOnPositiveButtonClick, Runnable runOnNegativeButtonClick) {
        return confirm(context, titleKey, messageKey, runOnPositiveButtonClick, runOnNegativeButtonClick,
                R.string.confirm_label);
    }

    public static AlertDialog confirm(Context context, int titleKey, int messageKey,
                                      final Runnable runOnPositiveButtonClick, Runnable runOnNegativeButtonClick,
                                      int positiveButtonLabelKey) {
        return confirm(context, titleKey, messageKey, runOnPositiveButtonClick, runOnNegativeButtonClick,
                positiveButtonLabelKey, android.R.string.cancel);
    }

    public static AlertDialog confirm(Context context, int titleKey, int messageKey,
                                      final Runnable runOnPositiveButtonClick, final Runnable runOnNegativeButtonClick,
                                      int positiveButtonLabelKey, int negativeButtonLabelKey) {
        return confirm(context, titleKey, context.getResources().getString(messageKey), runOnPositiveButtonClick, runOnNegativeButtonClick, positiveButtonLabelKey, negativeButtonLabelKey);
    }

    public static AlertDialog confirm(Context context, int titleKey, String message,
                                      final Runnable runOnPositiveButtonClick, final Runnable runOnNegativeButtonClick,
                                      int positiveButtonLabelKey, int negativeButtonLabelKey) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(context.getResources().getString(titleKey))
                .setMessage(message)
                .setPositiveButton(positiveButtonLabelKey, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        runOnPositiveButtonClick.run();
                    }
                })
                .setNegativeButton(negativeButtonLabelKey, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (runOnNegativeButtonClick != null) {
                            runOnNegativeButtonClick.run();
                        }
                    }
                })
                .create();
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(Context context) {
        return ProgressDialog.show(context, context.getString(R.string.processing),
                context.getString(R.string.please_wait), true);
    }

    public static void showProgressDialogWhile(final Context context, final Predicate<Void> predicate, final Runnable callback) {
        if (predicate.evaluate(null)) {
            callback.run();
        } else {
            final ProgressDialog progressDialog = showProgressDialog(context);
            Runnable predicateVerifier = new Runnable() {
                public void run() {
                    if (predicate.evaluate(null)) {
                        Tasks.runDelayed(this, 100);
                    } else {
                        progressDialog.dismiss();
                        callback.run();
                    }
                }
            };
            Tasks.runDelayed(predicateVerifier, 100);
        }
    }
}
