package org.openforis.collect.android.attributeconverter;

import org.openforis.collect.android.viewmodel.UiAttributeDefinition;
import org.openforis.collect.android.viewmodel.UiFileAttribute;
import org.openforis.collect.android.viewmodelmanager.NodeDto;
import org.openforis.idm.metamodel.FileAttributeDefinition;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.model.FileAttribute;
import org.openforis.idm.model.Value;

import java.io.File;

/**
 * @author Daniel Wiell
 */
class FileConverter extends AttributeConverter<FileAttribute, UiFileAttribute> {
    public UiFileAttribute uiAttribute(UiAttributeDefinition definition, FileAttribute attribute) {
        UiFileAttribute uiAttribute = new UiFileAttribute(attribute.getId(), isRelevant(attribute), definition);
        updateUiAttributeValue(attribute, uiAttribute);
        return uiAttribute;
    }

    protected void updateUiAttributeValue(FileAttribute attribute, UiFileAttribute uiAttribute) {
        org.openforis.idm.model.File file = attribute.getValue();
        if (file != null && file.getFilename() != null)
            uiAttribute.setFile(new File(file.getFilename()));
        else
            uiAttribute.setFile(null);
    }

    protected UiFileAttribute uiAttribute(NodeDto nodeDto, UiAttributeDefinition definition) {
        UiFileAttribute uiAttribute = new UiFileAttribute(nodeDto.id, nodeDto.relevant, definition);
        uiAttribute.setFile(nodeDto.file);
        return uiAttribute;
    }

    protected NodeDto dto(UiFileAttribute uiAttribute) {
        NodeDto dto = createDto(uiAttribute);
        dto.file = uiAttribute.getFile();
        return dto;
    }

    public Value value(UiFileAttribute uiAttribute) {
        File file = uiAttribute.getFile();

        return new org.openforis.idm.model.File(file == null ? null : file.getName(), file == null ? 0L : file.length());
    }

    protected FileAttribute attribute(UiFileAttribute uiAttribute, NodeDefinition definition) {
        FileAttribute a = new FileAttribute((FileAttributeDefinition) definition);
        if (!uiAttribute.isCalculated() || uiAttribute.isCalculatedOnlyOneTime())
            a.setValue((org.openforis.idm.model.File) value(uiAttribute));
        return a;
    }
}
