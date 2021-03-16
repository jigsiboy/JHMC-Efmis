package org.openforis.collect.android.gui.input;

import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;

import org.openforis.collect.android.SurveyService;
import org.openforis.collect.android.viewmodel.UiAttribute;
import org.openforis.collect.android.viewmodel.UiAttributeCollection;
import org.openforis.collect.android.viewmodel.UiNode;
import org.openforis.collect.android.viewmodel.UiTextAttribute;
import org.openforis.collect.android.viewmodel.UiValidationError;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Wiell
 */
public abstract class EditTextAttributeCollectionComponent extends AttributeCollectionComponent {
    private final Map<UiAttribute, EditTextAttributeComponent> attributeComponentByAttribute = new LinkedHashMap<UiAttribute, EditTextAttributeComponent>();
    private final LinearLayout view;

    protected EditTextAttributeCollectionComponent(UiAttributeCollection attributeCollection, SurveyService surveyService, FragmentActivity context) {
        super(attributeCollection, surveyService, context);
        view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        for (UiNode child : attributeCollection.getChildren())
            addAttributeToComponent((UiTextAttribute) child);
    }

    protected abstract EditTextAttributeComponent createAttributeComponent(UiTextAttribute attribute);

    protected final View.OnClickListener onAddAttribute() {
        return new View.OnClickListener() {
            public void onClick(View v) {
                UiTextAttribute attribute = (UiTextAttribute) surveyService.addAttribute();
                addAttributeToComponent(attribute);
                setValidationError(attribute, attribute.getValidationErrors());
            }
        };
    }

    protected final View toInputView() {
        return view;
    }

    protected final void setValidationError(UiAttribute attribute, Set<UiValidationError> validationErrors) {
        EditTextAttributeComponent attributeComponent = attributeComponentByAttribute.get(attribute);
        if (attributeComponent != null) // While we're adding an attribute, it's not in the map yet
            attributeComponent.setValidationError(validationErrors);
    }

    protected void resetValidationErrors() {
        for (AttributeComponent attributeComponent : attributeComponentByAttribute.values())
            attributeComponent.resetValidationErrors();
    }

    protected Set<UiAttribute> updateChangedAttributes() {
        Set<UiAttribute> changedAttributes = new HashSet<UiAttribute>();
        for (AttributeComponent attributeComponent : attributeComponentByAttribute.values())
            if (attributeComponent.updateAttributeIfChanged())
                changedAttributes.add(attributeComponent.attribute);
        return changedAttributes;
    }

    private void addAttributeToComponent(UiTextAttribute attribute) {
        EditTextAttributeComponent attributeComponent = createAttributeComponent(attribute);
        attributeComponentByAttribute.put(attribute, attributeComponent);
        View inputView = attributeComponent.toInputView();
        view.addView(inputView);
        showKeyboard(inputView);
    }
}

class TextAttributeCollectionComponent extends EditTextAttributeCollectionComponent {
    TextAttributeCollectionComponent(UiAttributeCollection attributeCollection, SurveyService surveyService, FragmentActivity context) {
        super(attributeCollection, surveyService, context);
    }

    protected TextAttributeComponent createAttributeComponent(UiTextAttribute attribute) {
        return new TextAttributeComponent(attribute, surveyService, context);
    }
}