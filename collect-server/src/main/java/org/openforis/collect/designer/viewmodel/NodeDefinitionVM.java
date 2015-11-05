/**
 * 
 */
package org.openforis.collect.designer.viewmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.designer.form.NodeDefinitionFormObject;
import org.openforis.collect.designer.model.AttributeType;
import org.openforis.collect.designer.model.NodeType;
import org.openforis.collect.designer.util.MessageUtil;
import org.openforis.collect.metamodel.ui.UIOptions;
import org.openforis.collect.metamodel.ui.UIOptions.Layout;
import org.openforis.collect.metamodel.ui.UITab;
import org.openforis.collect.metamodel.ui.UITabSet;
import org.openforis.idm.metamodel.AttributeDefinition;
import org.openforis.idm.metamodel.EntityDefinition;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.metamodel.NodeLabel.Type;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.Binder;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.DependsOn;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.proxy.FormProxyObject;
import org.zkoss.bind.proxy.MapProxy;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.IdSpace;
import org.zkoss.zk.ui.Path;

/**
 * 
 * @author S. Ricci
 *
 */
public abstract class NodeDefinitionVM<T extends NodeDefinition> extends SurveyObjectBaseVM<T> {

	protected static final String FORM_CONTAINER_ID = "nodeFormContainer";

	protected FormProxyObject tempFormObject;
	protected EntityDefinition parentEntity;

	public NodeDefinitionVM() {
		super();
		fieldLabelKeyPrefixes.addAll(Arrays.asList("survey.schema.node"));
	}
	
	protected void initInternal(EntityDefinition parentEntity, T nodeDefn, Boolean newItem) {
		super.init();
		tempFormObject = createTemporaryFormObject();
		if ( nodeDefn != null ) {
			this.parentEntity = parentEntity;
			this.newItem = newItem;
			setEditedItem(nodeDefn);
		}
	}

	protected FormProxyObject createTemporaryFormObject() {
		return new MapProxy<Object, Object>(new HashMap<Object, Object>(), null);
	}

	@Override
	protected T createItemInstance() {
		return null;
	}

	@Override
	protected void addNewItemToSurvey() {
		//do nothing
	}
	
	@Override
	protected void moveSelectedItemInSurvey(int indexTo) {
		//do nothing
	}
	
	@Override
	@GlobalCommand
	public void currentLanguageChanged() {
		super.currentLanguageChanged();
		setEditedItem(editedItem);
		notifyChange("tempFormObject","formObject");
	}
	
	@Override
	public void commitChanges(@ContextParam(ContextType.BINDER) Binder binder) {
		formObject.saveTo(editedItem, currentLanguageCode);
		boolean editingRootEntity = parentEntity == null;
		boolean wasNewItem = newItem;
		if ( wasNewItem ) {
			if ( editingRootEntity ) {
//				CollectSurvey survey = getSurvey();
//				UIOptions uiOptions = survey.getUIOptions();
//				uiOptions.createRootTabSet((EntityDefinition) editedItem);
//				Schema schema = editedItem.getSchema();
//				schema.addRootEntityDefinition((EntityDefinition) editedItem);
			} else {
				parentEntity.addChildDefinition(editedItem);
				validateForm(binder);
			}
			newItem = false;
		}
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("parentEntity", parentEntity);
		args.put("node", editedItem);
		args.put("newItem", wasNewItem);
		BindUtils.postGlobalCommand(null, null, "editedNodeChanged", args);
		dispatchSurveyChangedCommand();
	}

	@Command
	public void nameChanged(@ContextParam(ContextType.BINDER) Binder binder,
			@BindingParam("name") String name) {
		name = adjustInternalName(name);
		((NodeDefinitionFormObject<?>) formObject).setName(name);
		dispatchApplyChangesCommand(binder);
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("item", editedItem);
		args.put("name", name);
		BindUtils.postGlobalCommand(null, null, "editedNodeNameChanging", args);
	}

	@Command
	public void singleInstanceLabelChange(@ContextParam(ContextType.BINDER) Binder binder,
			@BindingParam("label") String value) {
		dispatchApplyChangesCommand(binder);
		
		((NodeDefinitionFormObject<?>) formObject).setInstanceLabel(value);
		String name = getFormFieldValue(tempFormObject, "name");
		if ( StringUtils.isBlank(name) && StringUtils.isNotBlank(value) ) {
			name = suggestInternalName(value);
			nameChanged(binder, name);
		}
	}
	
	protected String adjustInternalName(String name) {
		String result = super.adjustInternalName(name);
		setTempFormObjectFieldValue("name", result);
		return result;
	}
	
	protected String getInstanceLabel(NodeDefinition nodeDefn) {
		String label = nodeDefn.getLabel(Type.INSTANCE, currentLanguageCode);
		return label;
	}
	
	@Override
	protected List<T> getItemsInternal() {
		return null;
	}

	@Override
	protected void deleteItemFromSurvey(T item) {
		//do nothing
	}
	
	@GlobalCommand
	public void tabSetChanged(@ContextParam(ContextType.BINDER) Binder binder, @BindingParam("tabSet") UITabSet tabSet) {
		notifyChange("assignableTabNames");
		if ( isEditingItem() ) {
			validateForm(binder);
		}
	}
	
	@GlobalCommand
	public void tabChanged(@ContextParam(ContextType.BINDER) Binder binder, @BindingParam("tab") UITab tab ) {
		notifyChange("assignableTabNames");
	}
	
	@GlobalCommand
	public void closeVersioningManagerPopUp(@ContextParam(ContextType.BINDER) Binder binder) {
		validateForm(binder);
	}
	
	protected void validateForm(@ContextParam(ContextType.BINDER) Binder binder) {
		Component view = binder.getView();
		IdSpace currentIdSpace = view.getSpaceOwner();
		Component formComponent = Path.getComponent(currentIdSpace, FORM_CONTAINER_ID);
		Binder formComponentBinder = (Binder) formComponent.getAttribute("binder");
		formComponentBinder.postCommand("applyChanges", null);
	}
	
	// GETTERS AND SETTERS
	public FormProxyObject getTempFormObject() {
		return tempFormObject;
	}
	
	protected void setTempFormObjectFieldValue(String field, Object value) {
		setValueOnFormField(tempFormObject, field, value);
		BindUtils.postNotifyChange(null, null, tempFormObject, field);
	}
	
	@DependsOn("editedItem")
	public String getNodeType() {
		if ( editedItem != null ) {
			NodeType type = NodeType.valueOf(editedItem);
			return type.name();
		} else {
			return null;
		}
	}

	@DependsOn("editedItem")
	public String getAttributeType() {
		if ( editedItem != null && editedItem instanceof AttributeDefinition ) {
			AttributeType type = AttributeType.valueOf((AttributeDefinition) editedItem);
			return type.name();
		} else {
			return null;
		}
	}
	
	@DependsOn("editedItem")
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
	
	@DependsOn("editedItem")
	public boolean isRootEntity() {
		return editedItem != null && parentEntity == null;
	}
	
	@DependsOn("editedItem")
	public boolean isAttribute() {
		return editedItem != null && editedItem instanceof AttributeDefinition;
	}
	
	@DependsOn("editedItem")
	public boolean isInsideTableEntity() {
		UIOptions uiOptions = getSurvey().getUIOptions();
		return editedItem != null && parentEntity != null && parentEntity.isMultiple() && 
				uiOptions.getLayout(parentEntity) == Layout.TABLE; 
	}
	
	@DependsOn("editedItem")
	public boolean isMultipleFieldEditingDisabled() {
//		return editedItem == null || ! (editedItem instanceof CodeAttributeDefinition);
		return false;
	}
	
	public boolean isRequiredApplied() {
		return true;
	}
	
	public List<Map<String, String>> getDependentNodes() {
		List<Map<String, String>> result = new ArrayList<Map<String,String>>();
		try {
			result.addAll(getDependentNodeInfos("relevancy", editedItem.getRelevancyDependentDefinitions()));
			result.addAll(getDependentNodeInfos("min_count", editedItem.getMinCountDependentDefinitions()));
			result.addAll(getDependentNodeInfos("max_count", editedItem.getMaxCountDependentDefinitions()));
			result.addAll(getDependentNodeInfos("calculated_value", editedItem.getCalculatedValueDependentDefinitions()));
		} catch(Exception e) {
			//ignore
		}
		return result;
	}

	protected List<Map<String, String>> getDependentNodeInfos(String type, List<NodeDefinition> nodes) {
		List<Map<String, String>> result = new ArrayList<Map<String,String>>();
		for (NodeDefinition node : nodes) {
			Map<String, String> nodeInfo = new HashMap<String, String>();
			nodeInfo.put("type", type);
			nodeInfo.put("path", node.getPath());
			if ( ! result.contains(nodeInfo) ) {
				result.add(nodeInfo);
			}
		}
		return result;
	}
	
	protected boolean checkNodeAttached() {
		if (editedItem == null || editedItem.getParentDefinition() == null) {
			MessageUtil.showWarning("survey.schema.node.error.node_not_yet_attached");
			return false;
		} else {
			return true;
		}
	}

}
