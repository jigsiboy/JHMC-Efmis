package org.openforis.collect.android.gui.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.openforis.collect.R;

public class DefaultDialog extends Dialog {

    public static class Builder {
        private DefaultDialog dialog;
        public Builder(Context context) {
            dialog = new DefaultDialog(context);
        }

        public Builder title(int value) {
            dialog.setTitle(value);
            return this;
        }

        public Builder message(String value) {
            dialog.setMessage(value);
            return this;
        }

        public Builder message(int value) {
            dialog.setMessage(value);
            return this;
        }

        public Builder detail(String value) {
            dialog.setExtraDetail(value);
            return this;
        }

        public Builder detail(int value) {
            dialog.setExtraDetail(value);
            return this;
        }

        public Builder editTextFullName(String hint, int visibility){
            dialog.setFullNameEtText(hint, visibility);
            return this;
        }

        public Builder editTextEmail(String hint, int visibility){
            dialog.setEmailEtText(hint, visibility);
            return this;
        }

        public Builder positiveAction(String label, OnClickListener listener) {
            dialog.setPositiveButton(label, listener);
            return this;
        }

        public Builder negativeAction(String label, OnClickListener listener) {
            dialog.setNegativeButton(label, listener);
            return this;
        }

        public Builder hideEditText(int visibility) {
            dialog.setHidden(visibility);
            return this;
        }

        public DefaultDialog build() {
            dialog.shrinkContent();
            dialog.setCancelable(false);
            dialog.getEditTextEmail();
            dialog.getEditTextFullName();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            return dialog;
        }

    }

    private Button positiveButton;
    private Button negativeButton;
    private EditText etFullName;
    private EditText etEmail;

    public DefaultDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_default);
        positiveButton = (Button) findViewById(R.id.dialog_default_button_positive);
        negativeButton = (Button) findViewById(R.id.dialog_default_button_negative);
        etFullName = (EditText) findViewById(R.id.fullName);
        etEmail = (EditText) findViewById(R.id.email);
    }

    public interface OnClickListener {
        void onClick(Dialog dialog, String fullName, String email);
    }

    public void setMessage(String message) {
        ((TextView)findViewById(R.id.dialog_default_message)).setText(message);
    }

    public void setMessage(int messageId) {
        ((TextView)findViewById(R.id.dialog_default_message)).setText(messageId);
    }

    public void setExtraDetail(String extraDetail) {
        ((TextView)findViewById(R.id.dialog_default_extra_detail)).setText(extraDetail);
    }

    public void setExtraDetail(int extraDetail) {
        ((TextView)findViewById(R.id.dialog_default_extra_detail)).setText(extraDetail);
    }

    public void setFullNameEtText (String hint, int visibility){
        etFullName.setHint(hint);
        etFullName.setVisibility(visibility);
    }

    public void setEmailEtText (String hint, int visibility){
        etEmail.setHint(hint);
        etEmail.setVisibility(visibility);
    }

    public void setHidden(int visibility) {
        etEmail.setVisibility(visibility);
        etFullName.setVisibility(visibility);
    }


    public void setPositiveButton(String label, final OnClickListener listener) {
        positiveButton.setText(label);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(DefaultDialog.this, getEditTextFullName(), getEditTextEmail());
            }
        });
    }

    public void setNegativeButton(String label, final OnClickListener listener) {
        negativeButton.setText(label);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClick(DefaultDialog.this, "", "");
            }
        });
    }

    public String getEditTextFullName(){
        return etFullName.getText().toString();
    }

    public String getEditTextEmail(){
        return etEmail.getText().toString();
    }

    private boolean hasPositiveAction() {
        return !positiveButton.getText().toString().isEmpty();
    }

    private boolean hasNegativeAction() {
        return !negativeButton.getText().toString().isEmpty();
    }

    private void shrinkContent() {
        if (!hasPositiveAction()) {
            positiveButton.setVisibility(View.GONE);
        }
        if (!hasNegativeAction()) {
            negativeButton.setVisibility(View.GONE);
        }
    }
}
