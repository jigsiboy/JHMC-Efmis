package org.openforis.collect.android.viewmodelmanager;

import org.openforis.collect.android.DefinitionProvider;
import org.openforis.collect.android.attributeconverter.AttributeConverter;
import org.openforis.collect.android.gui.util.meter.Timer;
import org.openforis.collect.android.viewmodel.*;

import java.util.*;

import static org.openforis.collect.android.viewmodelmanager.NodeDto.Collection;

/**
 * @author Daniel Wiell
 */
public interface ViewModelRepository {
    void insertRecord(UiRecord record);

    UiRecord recordById(UiSurvey survey, int recordId);

    List<UiRecord.Placeholder> surveyRecords(int surveyId);

    void insertEntity(UiEntity entity, Map<Integer, StatusChange> statusChanges);

    void insertAttribute(UiAttribute attribute, Map<Integer, StatusChange> statusChanges);

    void updateAttribute(UiAttribute attribute, Map<Integer, StatusChange> statusChanges);

    void updateRecordModifiedOn(UiRecord record);

    void removeNode(UiNode node, Map<Integer, StatusChange> statusChanges);

    void removeRecord(int recordId);


    class DatabaseViewModelRepository implements ViewModelRepository {
        private final DefinitionProvider definitionProvider;
        private final NodeRepository repo;

        public DatabaseViewModelRepository(DefinitionProvider definitionProvider, NodeRepository repo) {
            this.definitionProvider = definitionProvider;
            this.repo = repo;
        }

        public void insertRecord(UiRecord record) {
            repo.insert(toNodeDtoList(record), new HashMap<Integer, StatusChange>());
        }

        public void updateRecordModifiedOn(UiRecord record) {
            repo.updateModifiedOn(toNodeDto(record));
        }

        public UiRecord recordById(UiSurvey survey, int recordId) {
            Collection nodeCollection = repo.recordNodes(recordId);
            return toRecord(survey, nodeCollection);
        }

        public List<UiRecord.Placeholder> surveyRecords(int surveyId) {
            Collection nodeCollection = repo.surveyRecords(surveyId);
            List<UiRecord.Placeholder> placeholders = new ArrayList<UiRecord.Placeholder>();
            List<NodeDto> recordNodes = nodeCollection.childrenOf(null);
            for (NodeDto recordNode : recordNodes)
                placeholders.add(
                        new UiRecord.Placeholder(
                                recordNode.id,
                                UiNode.Status.valueOf(recordNode.status),
                                recordNode.recordCollectionName,
                                definitionProvider.getById(recordNode.definitionId),
                                getRecordKeyAttributes(nodeCollection, recordNode),
                                recordNode.createdOn,
                                recordNode.modifiedOn
                        )
                );
            return placeholders;
        }

        private List<UiAttribute> getRecordKeyAttributes(Collection nodeCollection, NodeDto recordNode) {
            List<NodeDto> keyAttributeDtoList = nodeCollection.childrenOf(recordNode.id);
            List<UiAttribute> keyAttributes = new ArrayList<UiAttribute>();
            for (NodeDto keyAttributeDto : keyAttributeDtoList)
                keyAttributes.add((UiAttribute) toUiNode(keyAttributeDto));
            return keyAttributes;
        }

        public void insertEntity(UiEntity entity, final Map<Integer, StatusChange> statusChanges) {
            final List<NodeDto> nodes = toNodeDtoList(entity);

            Timer.time(NodeRepository.class, "insert", new Runnable() {
                public void run() {
                    repo.insert(nodes, statusChanges);
                }
            });
        }

        public void insertAttribute(UiAttribute attribute, final Map<Integer, StatusChange> statusChanges) {
            repo.insert(Arrays.asList(uiAttributeToDto(attribute)), statusChanges);
        }

        public void updateAttribute(UiAttribute attribute, Map<Integer, StatusChange> statusChanges) {
            repo.update(uiAttributeToDto(attribute), statusChanges);
        }

        public void removeNode(UiNode node, Map<Integer, StatusChange> statusChanges) {
            repo.removeAll(toIds(node), statusChanges);
        }

        public void removeRecord(int recordId) {
            repo.removeRecord(recordId);
        }

        private List<Integer> toIds(UiNode node) {
            List<Integer> ids = new ArrayList<Integer>();
            ids.add(node.getId());
            if (node instanceof UiInternalNode)
                for (UiNode childNode : ((UiInternalNode) node).getChildren())
                    ids.addAll(toIds(childNode));
            return ids;
        }

        private UiRecord toRecord(UiSurvey survey, Collection nodeCollection) {
            NodeDto recordNode = nodeCollection.getRootNode();
            UiRecordCollection recordCollection = survey.lookupRecordCollection(recordNode.recordCollectionName);
            Definition definition = definitionProvider.getById(recordNode.definitionId);
            UiRecord record = new UiRecord(recordNode.id, definition, recordCollection,
                    (UiRecord.Placeholder) recordCollection.getChildById(recordNode.id));
            record.setStatus(UiNode.Status.valueOf(recordNode.status));
            record.setCreatedOn(recordNode.createdOn);
            record.setModifiedOn(recordNode.modifiedOn);
            addChildNodes(record, nodeCollection);
            record.init();
            return record;
        }

        private void addChildNodes(UiInternalNode parentNode, Collection nodeCollection) {
            List<NodeDto> childNodeDtoList = nodeCollection.childrenOf(parentNode.getId());
            for (NodeDto nodeDto : childNodeDtoList) {
                UiNode child = toUiNode(nodeDto);
                child.setStatus(UiNode.Status.valueOf(nodeDto.status));
                parentNode.addChild(child);
                if (child instanceof UiInternalNode)
                    addChildNodes((UiInternalNode) child, nodeCollection);
            }
        }

        // TODO: Move conversion logic somewhere else
        private UiNode toUiNode(NodeDto nodeDto) {
            Definition definition = definitionProvider.getById(nodeDto.definitionId);
            switch (nodeDto.type) {
                case ENTITY:
                    return new UiEntity(nodeDto.id, nodeDto.relevant, definition);
                case INTERNAL_NODE:
                    return new UiInternalNode(nodeDto.id, nodeDto.relevant, definition);
                case ENTITY_COLLECTION:
                    return new UiEntityCollection(nodeDto.id, nodeDto.parentEntityId, nodeDto.relevant, definition);
                case ATTRIBUTE_COLLECTION:
                    return new UiAttributeCollection(nodeDto.id, nodeDto.parentEntityId, nodeDto.relevant, (UiAttributeCollectionDefinition) definition);
                default:
                    return AttributeConverter.toUiAttribute(nodeDto, (UiAttributeDefinition) definition);
            }
        }

        private List<NodeDto> toNodeDtoList(UiNode uiNode) {
            List<NodeDto> nodes = new ArrayList<NodeDto>();
            NodeDto node = toNodeDto(uiNode);
            node.status = uiNode.getStatus().name();
            nodes.add(node);
            if (uiNode instanceof UiInternalNode) {
                for (UiNode childUiNode : ((UiInternalNode) uiNode).getChildren())
                    nodes.addAll(toNodeDtoList(childUiNode));
            }
            return nodes;
        }

        private NodeDto toNodeDto(UiNode uiNode) {
            if (uiNode instanceof UiRecord)
                return uiRecordToDto((UiRecord) uiNode);
            if (uiNode instanceof UiEntity)
                return uiEntityToDto((UiEntity) uiNode);
            if (uiNode instanceof UiEntityCollection)
                return uiEntityCollectionToDto((UiEntityCollection) uiNode);
            if (uiNode instanceof UiAttributeCollection)
                return uiAttributeCollectionToDto((UiAttributeCollection) uiNode);
            if (uiNode instanceof UiInternalNode)
                return uiNodeToDto(uiNode);
            if (uiNode instanceof UiAttribute)
                return uiAttributeToDto((UiAttribute) uiNode);
            throw new IllegalStateException("Unexpected uiNode type: " + uiNode);
        }

        private NodeDto uiEntityCollectionToDto(UiEntityCollection uiEntityCollection) {
            NodeDto dto = uiNodeToDto(uiEntityCollection);
            dto.parentEntityId = uiEntityCollection.getParentEntityId();
            return dto;
        }

        private NodeDto uiAttributeCollectionToDto(UiAttributeCollection uiAttributeCollection) {
            NodeDto dto = uiNodeToDto(uiAttributeCollection);
            dto.parentEntityId = uiAttributeCollection.getParentEntityId();
            return dto;
        }

        private NodeDto uiRecordToDto(UiRecord uiRecord) {
            NodeDto dto = uiNodeToDto(uiRecord);
            dto.parentId = null;
            dto.definitionId = uiRecord.getDefinition().id;
            dto.recordCollectionName = uiRecord.getParent().getDefinition().name;
            return dto;
        }

        private NodeDto uiAttributeToDto(UiAttribute attribute) {
            return AttributeConverter.toDto(attribute);
        }

        private NodeDto uiEntityToDto(UiEntity entity) {
            return uiNodeToDto(entity);
        }

        private NodeDto uiNodeToDto(UiNode node) {
            NodeDto dto = new NodeDto();
            dto.id = node.getId();
            dto.definitionId = node.getDefinition().id;
            dto.parentId = node.getParent().getId();
            dto.surveyId = node.getUiSurvey().getId();
            dto.recordId = node.getUiRecord().getId();
            dto.type = NodeDto.Type.ofUiNode(node);
            dto.relevant = node.isRelevant();
            dto.createdOn = node.getCreatedOn();
            dto.modifiedOn = node.getModifiedOn();
            return dto;
        }
    }
}