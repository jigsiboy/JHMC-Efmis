package org.openforis.collect.android.gui.input;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.FragmentActivity;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.android.SurveyService;
import org.openforis.collect.android.viewmodel.UiAttribute;

/**
 * @author Daniel Wiell
 */
public abstract class EditTextAttributeComponent<T extends UiAttribute> extends AttributeComponent<T> {

    protected EditText editText;

    protected EditTextAttributeComponent(T attribute, SurveyService surveyService, FragmentActivity context) {
        super(attribute, surveyService, context);
        initializeInputView();
    }

    protected void initializeInputView() {
        initializeEditText();
    }

    protected EditText initializeEditText() {
        editText = createEditText();
        return editText;
    }

    protected abstract String attributeValue();

    protected String editTextToAttributeValue(String text) {
        return text;
    }

    protected abstract void updateAttributeValue(String newValue);

    protected abstract void onEditTextCreated(EditText input);

    protected TextView errorMessageContainerView() {
        return editText;
    }

    protected final boolean updateAttributeIfChanged() {
        String newValue = getEditTextString();
        if (StringUtils.isEmpty(newValue)) newValue = null;

        String newAttributeValue = editTextToAttributeValue(newValue);
        if (hasChanged(newAttributeValue)) {
            updateAttributeValue(newAttributeValue);
            return true;
        } else {
            return false;
        }
    }

    protected EditText getEditText() {
        return editText;
    }

    protected View toInputView() {
        return editText;
    }

    public final View getDefaultFocusedView() {
        return editText;
    }

    protected boolean hasChanged(String newValue) {
        if (newValue == null)
            return !attribute.isEmpty();
        return !StringUtils.equals(attributeValue(), newValue);
    }

    private String getEditTextString() {
        return editText.getText() == null ? null : editText.getText().toString();
    }

    protected String formattedAttributeValue() {
        return attributeValue();
    }

    protected EditText createEditText() {
        final EditText editText = new AppCompatEditText(context);
        editText.setSingleLine();

        editText.setText(formattedAttributeValue());

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    saveNode();
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT)
                    saveNode();
                return false;
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                editText.setError(null);
                delaySaveNode();
            }
        });
        onEditTextCreated(editText);
        return editText;
    }
}
