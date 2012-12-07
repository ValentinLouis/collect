/**
 * 
 */
package org.openforis.collect.designer.viewmodel;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.designer.component.SchemaTreeModel;
import org.openforis.collect.designer.component.SchemaTreeModel.SchemaTreeNodeData;
import org.openforis.collect.designer.model.AttributeType;
import org.openforis.collect.designer.model.NodeType;
import org.openforis.collect.designer.util.MessageUtil;
import org.openforis.collect.designer.util.Resources;
import org.openforis.collect.metamodel.ui.UIOptions;
import org.openforis.collect.metamodel.ui.UIOptions.Layout;
import org.openforis.collect.metamodel.ui.UITabSet;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.idm.metamodel.AttributeDefinition;
import org.openforis.idm.metamodel.EntityDefinition;
import org.openforis.idm.metamodel.ModelVersion;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.metamodel.NodeLabel.Type;
import org.openforis.idm.metamodel.Schema;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.Binder;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.DependsOn;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.IdSpace;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Include;

/**
 * 
 * @author S. Ricci
 *
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class SchemaVM extends SurveyBaseVM {

	private static final boolean FILTER_BY_ROOT_ENTITY = true;
	private static final boolean INCLUDE_ROOT_ENTITY_IN_TREE = true;

	private static final String SCHEMA_CHANGED_GLOBAL_COMMAND = "schemaChanged";
	private static final String VALIDATE_COMMAND = "validate";
	
	private static final String CONFIRM_REMOVE_NODE_MESSAGE_KEY = "survey.schema.confirm_remove_node";
	private static final String CONFIRM_REMOVE_NON_EMPTY_ENTITY_MESSAGE_KEY = "survey.schema.confirm_remove_non_empty_entity";
	private static final String CONFIRM_REMOVE_NODE_TITLE_KEY = "survey.schema.confirm_remove_node_title";

	private SchemaTreeNodeData selectedTreeNode;
	private NodeDefinition editedNode;
	private boolean newNode;

	private EntityDefinition selectedRootEntity;
	private ModelVersion selectedVersion;
	
	@Wire
	private Include nodeFormInclude;
	
	private SchemaTreeModel treeModel;

	private EntityDefinition editedNodeParentEntity;

	@Override
	@Init(superclass=false)
	public void init() {
		super.init();
	}
	
	@AfterCompose
	public void afterCompose(@ContextParam(ContextType.VIEW) Component view){
		 Selectors.wireComponents(view, this, false);
		 Selectors.wireEventListeners(view, this);
	}
	
	@Command
	public void nodeSelected(@ContextParam(ContextType.BINDER) final Binder binder, @BindingParam("data") final SchemaTreeNodeData data) {
		if ( data != null ) {
			checkCanLeaveForm(new MessageUtil.CompleteConfirmHandler() {
				@Override
				public void onOk() {
					undoLastChanges();
					performSelectNode(binder, data);
				}
				@Override
				public void onCancel() {
					treeModel.select(selectedTreeNode);
				}
			});
		} else {
			resetEditingStatus();
		}
	}

	@Command
	public void rootEntitySelected(@BindingParam("rootEntity") final EntityDefinition rootEntity) {
		nodesTreeFilterChanged(rootEntity, selectedVersion);
	}
	
	@Command
	@NotifyChange({"selectedTreeNode","treeModel","selectedVersion"})
	public void versionSelected(@BindingParam("version") ModelVersion version) {
		nodesTreeFilterChanged(selectedRootEntity, version);
	}
	
	protected void nodesTreeFilterChanged(final EntityDefinition rootEntity, final ModelVersion version) {
		if ( checkCanLeaveForm(new MessageUtil.CompleteConfirmHandler() {
			@Override
			public void onOk() {
				selectedRootEntity = rootEntity;
				selectedVersion = version;
				resetEditingStatus();
				updateTreeModel();
				dispatchCurrentFormValidatedCommand(true, isCurrentFormBlocking());
				notifyChange("selectedTreeNode","selectedRootEntity","selectedVersion");
			}
			@Override
			public void onCancel() {
				notifyChange("selectedRootEntity","selectedVersion");
			}
		}));
	}

	protected void performSelectNode(Binder binder, SchemaTreeNodeData data) {
		selectedTreeNode = data;
		treeModel.select(data);
		NodeDefinition nodeDefn = data.getNodeDefinition();
		EntityDefinition parentDefn = nodeDefn == null ? null : (EntityDefinition) nodeDefn.getParentDefinition();
		editNode(binder, false, parentDefn, nodeDefn);
	}

	@Command
	public void addRootEntity(@ContextParam(ContextType.BINDER) final Binder binder) {
		checkCanLeaveForm(new MessageUtil.ConfirmHandler() {
			@Override
			public void onOk() {
				resetNodeSelection();
				EntityDefinition newNode = createRootEntityDefinition();
				selectedRootEntity = newNode;
				selectedVersion = null;
				editNode(binder, true, null, newNode);
				updateTreeModel();
				selectTreeNode(newNode);
				notifyChange("selectedRootEntity","selectedVersion");
			}
		});
	}

	@Command
	public void addEntity(@ContextParam(ContextType.BINDER) final Binder binder, 
			@BindingParam("multiple") boolean multiple, 
			@BindingParam("layout") String layout) {
		resetNodeSelection();
		addChildEntity(binder, multiple, layout);
	}

	@Command
	public void addChildEntity(@ContextParam(ContextType.BINDER) final Binder binder,
			@BindingParam("multiple") final boolean multiple, 
			@BindingParam("layout") final String layout) {
		checkCanLeaveForm(new MessageUtil.ConfirmHandler() {
			@Override
			public void onOk() {
				EntityDefinition newNode = createEntityDefinition();
				newNode.setMultiple(multiple);
				UIOptions uiOpts = survey.getUIOptions();
				EntityDefinition parentEntity = (EntityDefinition) (selectedTreeNode == null ? selectedRootEntity: selectedTreeNode.getNodeDefinition());
				Layout layoutEnum = Layout.valueOf(layout);
//				if ( uiOpts.isLayoutSupported(parentEntity, newNode.getId(), (UITab) null, multiple, layoutEnum) ) {
					uiOpts.setLayout(newNode, layoutEnum);
					editNode(binder, true, parentEntity, newNode);
//				} else {
//					MessageUtil.showWarning(LabelKeys.LAYOUT_NOT_SUPPORTED_MESSAGE_KEY);
//				}
				afterNewNodeCreated(newNode);
			}
		});
	}
	
	@Command
	public void addAttribute(@ContextParam(ContextType.BINDER) final Binder binder, 
			@BindingParam("attributeType") final String attributeType) throws Exception {
		resetNodeSelection();
		addChildAttribute(binder, attributeType);
	}
	
	@Command
	public void addChildAttribute(@ContextParam(ContextType.BINDER) final Binder binder, 
			@BindingParam("attributeType") final String attributeType) throws Exception {
		checkCanLeaveForm(new MessageUtil.ConfirmHandler() {
			@Override
			public void onOk() {
				AttributeType attributeTypeEnum = AttributeType.valueOf(attributeType);
				AttributeDefinition newNode = (AttributeDefinition) NodeType.createNodeDefinition(survey, NodeType.ATTRIBUTE, attributeTypeEnum);
				EntityDefinition parentEntity = (EntityDefinition) (selectedTreeNode == null ? selectedRootEntity: selectedTreeNode.getNodeDefinition());
				editNode(binder, true, parentEntity, newNode);
				afterNewNodeCreated(newNode);
			}
		});
	}

	private void afterNewNodeCreated(NodeDefinition nodeDefn) {
		treeModel.select(editedNodeParentEntity);
		treeModel.appendNodeToSelected(nodeDefn, true);
		selectTreeNode(nodeDefn);
		//workaround: tree nodes not refreshed when adding child to "leaf" nodes (i.e. empty entity)
		notifyChange("treeModel");
	}

	@Override
	@GlobalCommand
	public void undoLastChanges() {
		if ( editedNode != null ) {
			if ( newNode ) {
				treeModel.select(editedNode);
				treeModel.removeSelectedNode();
			}
			resetEditingStatus();
		}
	}

	protected void resetEditingStatus() {
		resetNodeSelection();
		editedNode = null;
		notifyChange("selectedTreeNode","editedNode");
	}
	
	protected void resetNodeSelection() {
		selectedTreeNode = null;
		notifyChange("selectedTreeNode");
		resetTreeSelection();
	}
	
	protected void resetTreeSelection() {
		if ( treeModel != null ) {
			treeModel.deselect();
		}
	}
	
	protected void selectTreeNode(NodeDefinition nodeDefn) {
		treeModel.select(nodeDefn);
		selectedTreeNode = treeModel.getNodeData(nodeDefn);
		BindUtils.postNotifyChange(null, null, selectedTreeNode, "*");
	}
	
	@Override
	@GlobalCommand
	public void versionsUpdated() {
		super.versionsUpdated();
		if ( selectedVersion != null && ! survey.getVersions().contains(selectedVersion) ) {
			selectedVersion = null;
			buildTreeModel();
			notifyChange("selectedVersion");
		}
	}
	
	protected void editNode(Binder binder, boolean newNode, EntityDefinition parentEntity, NodeDefinition node) {
		this.newNode = newNode;
		editedNodeParentEntity = parentEntity;
		editedNode = node;
		if ( ! newNode ) {
			selectedTreeNode = treeModel.getNodeData(node);
		}
		refreshNodeForm(parentEntity);
		validateForm(binder);
		notifyChange("selectedTreeNode","editedNode");
	}

	protected void refreshNodeForm(EntityDefinition parentEntity) {
		nodeFormInclude.setSrc(null);
		if ( editedNode != null ) {
			nodeFormInclude.setDynamicProperty("parentEntity", parentEntity);
			nodeFormInclude.setDynamicProperty("item", editedNode);
			nodeFormInclude.setDynamicProperty("newItem", newNode);
			String location;
			if ( editedNode instanceof EntityDefinition ) {
				location = Resources.Component.ENTITY.getLocation();
			} else {
				AttributeType attributeType = AttributeType.valueOf((AttributeDefinition) editedNode);
				location = Resources.Component.ATTRIBUTE.getLocation();
				String attributeTypeShort = attributeType.name().toLowerCase();
				location = MessageFormat.format(location, attributeTypeShort);
			}
			nodeFormInclude.setSrc(location);
		}
	}
	
	protected void validateForm() {
		if ( editedNode != null ) {
			Binder binder = (Binder) nodeFormInclude.getAttribute("$BINDER$");
			validateForm(binder);
		} else {
			dispatchCurrentFormValidatedCommand(true);
		}
	}
		
	protected void validateForm(@ContextParam(ContextType.BINDER) Binder binder) {
		Component view = binder.getView();
		IdSpace currentIdSpace = view.getSpaceOwner();
		Component formComponent = Path.getComponent(currentIdSpace, "nodeFormInclude/nodeFormContainer");
		Binder formComponentBinder = (Binder) formComponent.getAttribute("binder");
		formComponentBinder.postCommand(VALIDATE_COMMAND, null);
	}

	@Command
	public void removeNode() {
		removeNode(selectedTreeNode);
	}

	public void removeNode(final SchemaTreeNodeData data) {
		final NodeDefinition nodeDefn = data.getNodeDefinition();
		if ( nodeDefn != null ) {
			String confirmMessageKey;
			if (nodeDefn instanceof EntityDefinition && !((EntityDefinition) nodeDefn).getChildDefinitions().isEmpty() ) {
				confirmMessageKey = CONFIRM_REMOVE_NON_EMPTY_ENTITY_MESSAGE_KEY;
			} else {
				confirmMessageKey = CONFIRM_REMOVE_NODE_MESSAGE_KEY;
			}
			NodeType type = NodeType.valueOf(nodeDefn);
			String typeLabel = type.getLabel().toLowerCase();
			boolean isRootEntity = nodeDefn.getParentDefinition() == null;
			if ( isRootEntity ) {
				typeLabel = Labels.getLabel("survey.schema.root_entity");
			}
			Object[] messageArgs = new String[] {typeLabel, nodeDefn.getName()};
			Object[] titleArgs = new String[] {typeLabel};
			MessageUtil.showConfirm(new MessageUtil.ConfirmHandler() {
				@Override
				public void onOk() {
					performRemoveNode(nodeDefn);
				}
			}, confirmMessageKey, messageArgs, CONFIRM_REMOVE_NODE_TITLE_KEY, titleArgs);
		}
	}
	
	@Command
	@NotifyChange({"moveNodeUpDisabled","moveNodeDownDisabled"})
	public void moveNodeUp() {
		moveNode(true);
	}
	
	@Command
	@NotifyChange({"moveNodeUpDisabled","moveNodeDownDisabled"})
	public void moveNodeDown() {
		moveNode(false);
	}
	
	protected void moveNode(boolean up) {
		List<NodeDefinition> siblings = getSiblingsInTree(selectedTreeNode);
		NodeDefinition selectedNodeDefn = selectedTreeNode.getNodeDefinition();
		int oldIndex = siblings.indexOf(selectedNodeDefn);
		int newIndex = up ? oldIndex - 1: oldIndex + 1;
		moveNode(newIndex);
		treeModel.moveSelectedNode(newIndex);
		dispatchSchemaChangedCommand();
	}
	
	protected void moveNode(int newIndexInTree) {
		List<NodeDefinition> siblings = getSiblingsInTree(selectedTreeNode);
		NodeDefinition selectedNodeDefn = selectedTreeNode.getNodeDefinition();
		EntityDefinition parentDefn = (EntityDefinition) selectedNodeDefn.getParentDefinition();
		NodeDefinition treeNodeToMoveInto = siblings.get(newIndexInTree);
		int toIndex;
		if ( treeNodeToMoveInto != null ) {
			toIndex = parentDefn.getChildDefinitionIndex(treeNodeToMoveInto);
		} else {
			toIndex = newIndexInTree;
		}
		if ( parentDefn != null ) {
			parentDefn.moveChildDefinition(selectedNodeDefn, toIndex);
		} else {
			EntityDefinition rootEntity = selectedNodeDefn.getRootEntity();
			Schema schema = rootEntity.getSchema();
			schema.moveRootEntityDefinition(rootEntity, toIndex);
		}
	}
	
	protected void performRemoveNode(NodeDefinition nodeDefn) {
		EntityDefinition parentDefn = (EntityDefinition) nodeDefn.getParentDefinition();
		if ( parentDefn != null ) {
			parentDefn.removeChildDefinition(nodeDefn);
			if ( treeModel != null ) {
				treeModel.removeSelectedNode();
				notifyChange("treeModel");
			}
		} else {
			UIOptions uiOpts = survey.getUIOptions();
			UITabSet tabSet = uiOpts.getAssignedRootTabSet((EntityDefinition) nodeDefn);
			uiOpts.removeTabSet(tabSet);
			Schema schema = nodeDefn.getSchema();
			String nodeName = nodeDefn.getName();
			schema.removeRootEntityDefinition(nodeName);
			selectedRootEntity = null;
			notifyChange("selectedRootEntity");
			updateTreeModel();
		}
		resetEditingStatus();
		dispatchCurrentFormValidatedCommand(true);
		dispatchSchemaChangedCommand();
	}

	@GlobalCommand
	@NotifyChange({"selectedTreeNode","newNode"})
	public void editedNodeChanged(@BindingParam("parentEntity") EntityDefinition parentEntity) {
		if ( newNode ) {
			if ( parentEntity == null ) {
				selectedRootEntity = (EntityDefinition) editedNode;
				notifyChange("selectedRootEntity","selectedVersion");
				updateTreeModel();
			} else {
				selectedTreeNode.setDetached(false);
				BindUtils.postNotifyChange(null, null, selectedTreeNode, "detached");
			}
			selectedTreeNode = treeModel.getNodeData(editedNode);
			treeModel.select(selectedTreeNode);
			newNode = false;
			notifyChange("newNode");
		}
		dispatchSchemaChangedCommand();
	}
	
	protected EntityDefinition createRootEntityDefinition() {
		EntityDefinition newNode = createEntityDefinition();
		return newNode;
	}

	protected EntityDefinition createEntityDefinition() {
		Schema schema = survey.getSchema();
		EntityDefinition newNode = schema.createEntityDefinition();
		return newNode;
	}
	
	protected void dispatchSchemaChangedCommand() {
		BindUtils.postGlobalCommand(null, null, SCHEMA_CHANGED_GLOBAL_COMMAND, null);
	}
	
	public SchemaTreeModel getTreeModel() {
		if ( treeModel == null ) {
			buildTreeModel();
		}
		return treeModel;
    }

	protected void buildTreeModel() {
		CollectSurvey survey = getSurvey();
		if ( survey == null ) {
			//TODO session expired...?
		} else {
			treeModel = SchemaTreeModel.createInstance(selectedRootEntity, selectedVersion, INCLUDE_ROOT_ENTITY_IN_TREE, true);
		}
	}

	protected boolean isVersionSelected() {
		return survey.getVersions().isEmpty() || selectedVersion != null;
	}
	
	protected void updateTreeModel() {
		buildTreeModel();
		notifyChange("treeModel");
	}
	
	public boolean isEntity(NodeDefinition nodeDefn) {
		return nodeDefn instanceof EntityDefinition;
	}
	
	protected List<NodeDefinition> getSiblingsInTree(SchemaTreeNodeData data) {
		NodeDefinition nodeDefn = data.getNodeDefinition();
		List<NodeDefinition> result = new ArrayList<NodeDefinition>();
		EntityDefinition parentDefn = (EntityDefinition) nodeDefn.getParentDefinition();
		List<? extends NodeDefinition> allSiblings;
		if ( parentDefn == null ) {
			if ( FILTER_BY_ROOT_ENTITY ) {
				allSiblings = Arrays.asList(selectedRootEntity);
			} else {
				Schema schema = selectedRootEntity.getSchema();
				allSiblings = schema.getRootEntityDefinitions();
			}
		} else {
			allSiblings = parentDefn.getChildDefinitions();
		}
		//filter siblings
		for (NodeDefinition sibling : allSiblings) {
			if ( selectedVersion == null || selectedVersion.isApplicable(sibling) ) {
				result.add(sibling);
			}
		}
		return result;
	}
	
	@DependsOn("selectedTreeNode")
	public boolean isMoveNodeUpDisabled() {
		return isMoveNodeDisabled(true);
	}
	
	@DependsOn("selectedTreeNode")
	public boolean isMoveNodeDownDisabled() {
		return isMoveNodeDisabled(false);
	}
	
	protected boolean isMoveNodeDisabled(boolean up) {
		if ( ! newNode && selectedTreeNode != null ) {
			List<NodeDefinition> siblings = getSiblingsInTree(selectedTreeNode);
			NodeDefinition selectedNodeDefn = selectedTreeNode.getNodeDefinition();
			int index = siblings.indexOf(selectedNodeDefn);
			return isMoveItemDisabled(siblings, index, up);
		} else {
			return true;
		}
	}

	protected boolean isMoveItemDisabled(List<?> siblings, int index, boolean up) {
		return up ? index <= 0: index < 0 || index >= siblings.size() - 1;
	}
	
	@DependsOn({"newNode","editedNode"})
	public String getNodeTypeHeaderLabel() {
		String result = null;
		String nodeTypeStr = getNodeType();
		String messageKey;
		if ( nodeTypeStr != null ) {
			String nodeTypeLabel = null;
			NodeType nodeType = NodeType.valueOf(nodeTypeStr);
			switch (nodeType) {
			case ENTITY:
				if ( editedNodeParentEntity == null ) {
					messageKey = "survey.schema.node_detail_title.root_entity";
				} else {
					messageKey = "survey.schema.node_detail_title.entity";
				}
				nodeTypeLabel = Labels.getLabel(messageKey);
				break;
			case ATTRIBUTE:
				messageKey = "survey.schema.node_detail_title.attribute";
				Object[] args = new String[]{getAttributeTypeLabel()};
				nodeTypeLabel = Labels.getLabel(messageKey, args);
				break;
			}
			if ( newNode ) {
				Object[] args = new String[]{nodeTypeLabel.toLowerCase()};
				result = Labels.getLabel("survey.schema.node_detail_title.new_node", args);
			} else {
				result = nodeTypeLabel;
			}
		}
		return result;
	}
	
	@DependsOn({"newNode"})
	public String getNewTreeNodeLabel() {
		if ( newNode ) {
			String nodeTypeLabel = getNodeTypeHeaderLabel();
			Object[] args = new String[]{nodeTypeLabel};
			String result = Labels.getLabel("survey.schema.tree.new_node_label", args);
			return result;
		} else {
			return null;
		}
	}
	
	@DependsOn("editedNode")
	public String getNodeType() {
		if ( editedNode != null ) {
			NodeType type = NodeType.valueOf(editedNode);
			return type.name();
		} else {
			return null;
		}
	}

	@DependsOn("editedNode")
	public String getAttributeType() {
		if ( editedNode != null && editedNode instanceof AttributeDefinition ) {
			AttributeType type = AttributeType.valueOf((AttributeDefinition) editedNode);
			return type.name();
		} else {
			return null;
		}
	}
	
	@DependsOn("editedNode")
	public String getAttributeTypeLabel() {
		String type = getAttributeType();
		return getAttributeTypeLabel(type);
	}

	public String getAttributeTypeLabel(String typeValue) {
		if ( StringUtils.isNotBlank(typeValue) ) {
			AttributeType type = AttributeType.valueOf(typeValue);
			return type.getLabel();
		} else {
			return null;
		}
	}
	
	public List<String> getAttributeTypeValues() {
		List<String> result = new ArrayList<String>();
		AttributeType[] values = AttributeType.values();
		for (AttributeType type : values) {
			result.add(type.name());
		}
		return result;
	}
	
	public String getAttributeTypeLabelFromDefinition(AttributeDefinition attrDefn) {
		if ( attrDefn != null ) {
			AttributeType type = AttributeType.valueOf(attrDefn);
			return type.getLabel();
		} else {
			return null;
		}
	}
	
	public String getAttributeInstanceLabel(AttributeDefinition attrDefn) {
		return attrDefn.getLabel(Type.INSTANCE, currentLanguageCode);
	}
	
	public String getIcon(NodeDefinition nodeDefn) {
		String result = "assets/images/node_types/";
		if ( nodeDefn instanceof EntityDefinition ) {
			result += "entity";
		} else {
			AttributeType attributeType = AttributeType.valueOf((AttributeDefinition) nodeDefn);
			result += attributeType.name().toLowerCase();
		}
		result += "-small.png";
		return result;
	}
	
	public SchemaTreeNodeData getSelectedTreeNode() {
		return selectedTreeNode;
	}
	
	public NodeDefinition getEditedNode() {
		return editedNode;
	}
	
	@DependsOn("editedNode")
	public boolean isEditingNode() {
		return editedNode != null;
	}

	public boolean isNewNode() {
		return newNode;
	}

	public EntityDefinition getSelectedRootEntity() {
		return selectedRootEntity;
	}

	public ModelVersion getSelectedVersion() {
		return selectedVersion;
	}

}
