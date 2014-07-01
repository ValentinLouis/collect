package org.openforis.collect.metamodel.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.openforis.collect.metamodel.ui.Table.Direction;
import org.openforis.collect.metamodel.ui.UIOptions.CoordinateAttributeFieldsOrder;
import org.openforis.collect.metamodel.ui.UIOptions.Layout;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.idm.metamodel.AttributeDefinition;
import org.openforis.idm.metamodel.CoordinateAttributeDefinition;
import org.openforis.idm.metamodel.EntityDefinition;
import org.openforis.idm.metamodel.LanguageSpecificText;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.metamodel.NodeDefinitionVisitor;
import org.openforis.idm.metamodel.NodeLabel;
import org.openforis.idm.metamodel.NodeLabel.Type;
import org.openforis.idm.metamodel.Schema;
import org.openforis.idm.metamodel.TextAttributeDefinition;

/**
 * 
 * @author S. Ricci
 *
 */
@SuppressWarnings("deprecation")
public class UIOptionsMigrator {
	
	public UIConfiguration migrateToUIConfiguration(UIOptions oldUIOptions) {
		CollectSurvey survey = oldUIOptions.getSurvey();
		UIConfiguration result = new UIConfiguration(survey);
		List<UITabSet> tabSets = oldUIOptions.getTabSets();
		for (UITabSet tabSet : tabSets) {
			FormSet formSet;
			EntityDefinition associatedRootEntity = findAssociatedRootEntity(tabSet);
			if ( associatedRootEntity == null ) {
				throw new IllegalStateException("Cannot find associated root entity. Tab set: " + tabSet.getName());
			} else {
				formSet = result.createFormSet();
				formSet.setRootEntityDefinition(associatedRootEntity);
				result.addFormSet(formSet);
			}
			List<UITab> tabs = tabSet.getTabs();
			for (UITab tab : tabs) {
				createForm(formSet, tab);
			}
		}
		verifyMigration(result);
		return result;
	}

	private void verifyMigration(final UIConfiguration uiConfig) {
//		
//		StringWriter writer = new StringWriter();
//		UIConfigurationSerializer serializer = new UIConfigurationSerializer();
//		serializer.write(uiConfig, writer);
//		System.out.println(writer.toString());
//		
		CollectSurvey survey = uiConfig.getSurvey();
		Schema schema = survey.getSchema();
		schema.traverse(new NodeDefinitionVisitor() {
			@Override
			public void visit(NodeDefinition definition) {
				int nodeId = definition.getId();
				UIModelObject uiModelObj = uiConfig.getModelObjectByNodeDefinitionId(nodeId);
				if ( uiModelObj == null ) {
					throw new IllegalStateException(String.format("No UI model object found for node with id %d", nodeId));
				}
			}
		});

	}

	protected void createForm(FormContentContainer parent, UITab tab) {
		UIOptions oldUIOptions = tab.getUIOptions();
		Form form = parent.createForm();
		copyLabels(tab, form);

		//create form components
		List<NodeDefinition> childNodes = oldUIOptions.getNodesPerTab(tab, false);
		for (NodeDefinition nodeDefn : childNodes) {
			addFormComponent(form, nodeDefn);
		}
		
		//create inner forms
		List<UITab> nestedTabs = new ArrayList<UITab>();
		for (UITab childTab : tab.getTabs()) {
			boolean toBeAdded = true;
			List<NodeDefinition> innerNodes = oldUIOptions.getNodesPerTab(childTab, false);
			for (NodeDefinition nestedTabChildNode : innerNodes) {
				for (NodeDefinition childDefn : childNodes ) {
					if ( childDefn == nestedTabChildNode || 
							(childDefn instanceof EntityDefinition && nestedTabChildNode.isDescendantOf((EntityDefinition) childDefn) ) ) {
						toBeAdded = false;
						break;
					}
				}
			}
			if ( toBeAdded ) {
				nestedTabs.add(childTab);
			}
		}
		
		for (UITab uiTab : nestedTabs) {
			createForm(form, uiTab);
		}
		
		parent.addForm(form);
	}

	private void createInnerForms(UITab tab, FormSection parent) {
//		UIOptions oldUIOptions = tab.getUIOptions();
//		List<NodeDefinition> nodesPerTab = oldUIOptions.getNodesPerTab(tab, false);
//		for (NodeDefinition nodeDefn : nodesPerTab) {
//			if ( nodeDefn instanceof EntityDefinition && nodeDefn.isMultiple() && 
//					oldUIOptions.getLayout((EntityDefinition) nodeDefn) == Layout.FORM ) {
//				UITab assignedTab = oldUIOptions.getAssignedTab(nodeDefn);
//				if ( assignedTab == tab ) {
//					createFormFromFormEntity(assignedTab, parent, (EntityDefinition) nodeDefn);
//				}
//			}
//		}
		List<UITab> innerTabs = tab.getTabs();
		for (UITab innerTab : innerTabs) {
			createForm(parent, innerTab);
		}
	}

//	protected void createFormFromFormEntity(UITab parentTab, Form parent, EntityDefinition entityDefn) {
//		Form form = parent.createForm();
//		copyLabels(entityDefn, form);
//		form.setEntityId(entityDefn.getId());
//		form.setMultiple(true);
//		createMainFormSection(form, parentTab.getUIOptions(), entityDefn.getChildDefinitions());
//		createInnerForms(parentTab, form);
//		parent.addForm(form);
//	}

	protected EntityDefinition findAssignedEntityDefinition(UITab tab) {
		UIOptions oldUIOptions = tab.getUIOptions();
		List<NodeDefinition> nodesPerTab = oldUIOptions.getNodesPerTab(tab, false);
		if ( nodesPerTab.size() == 1 ) {
			NodeDefinition firstNode = nodesPerTab.get(0);
			if ( firstNode instanceof EntityDefinition ) {
				return (EntityDefinition) firstNode;
			}
		}
		return null;
	}

	protected void addFormComponent(FormContentContainer parent, NodeDefinition nodeDefn) {
		CollectSurvey survey = (CollectSurvey) nodeDefn.getSurvey();
		UIOptions oldUIOptions = survey.getUIOptions();
		FormComponent component;
		if ( nodeDefn instanceof AttributeDefinition ) {
			component = createField(parent, nodeDefn);
		} else {
			EntityDefinition entityDefn = (EntityDefinition) nodeDefn;
			if ( entityDefn.isMultiple() && oldUIOptions.getLayout(entityDefn) == Layout.TABLE ) {
				component = createTable(parent, entityDefn);
			} else {
				component = createFormSection(parent, entityDefn);
			}
		}
		parent.addChild(component);
	}

	protected Field createField(FormContentContainer parent, NodeDefinition nodeDefn) {
		CollectSurvey survey = (CollectSurvey) nodeDefn.getSurvey();
		UIOptions uiOptions = survey.getUIOptions();
		Field field = parent.createField();
		field.setAttributeDefinitionId(nodeDefn.getId());
		if ( nodeDefn instanceof TextAttributeDefinition ) {
			String autoCompleteGroup = uiOptions.getAutoCompleteGroup((TextAttributeDefinition) nodeDefn);
			field.setAutoCompleteGroup(autoCompleteGroup);
		}
		if ( nodeDefn instanceof CoordinateAttributeDefinition ) {
			CoordinateAttributeFieldsOrder fieldsOrder = uiOptions.getFieldsOrder((CoordinateAttributeDefinition) nodeDefn);
			field.setFieldsOrder(fieldsOrder);
		}
		return field;
	}

	protected FormSection createFormSection(FormContentContainer parent, EntityDefinition entityDefn) {
		CollectSurvey survey = (CollectSurvey) entityDefn.getSurvey();
		UIOptions uiOptions = survey.getUIOptions();
		UITab parentTab = uiOptions.getAssignedTab(entityDefn, true);
		
		FormSection formSection = parent.createFormSection();
		formSection.setEntityDefinition(entityDefn);

		List<NodeDefinition> childDefns = entityDefn.getChildDefinitions();
		for (NodeDefinition childDefn : childDefns) {
			UITab assignedChildTab = uiOptions.getAssignedTab(childDefn, true);
			if ( assignedChildTab == parentTab ) {
				addFormComponent(formSection, childDefn);
			}
		}
		
		//create inner tabs
		for (NodeDefinition innerChildDefn : childDefns) {
			UITab assignedInnerTab = uiOptions.getAssignedTab(innerChildDefn);
			if ( assignedInnerTab != null && assignedInnerTab.isDescendantOf(parentTab) ) {
				createInnerForms(parentTab, formSection);
				break;
			}
		}
		return formSection;
	}

	protected Table createTable(FormContentContainer section, EntityDefinition entityDefn) {
		Table table = section.createTable();
		table.setEntityDefinition(entityDefn);
		CollectSurvey survey = (CollectSurvey) entityDefn.getSurvey();
		UIOptions uiOptions = survey.getUIOptions();
		table.setCountInSummaryList(uiOptions.getCountInSumamryListValue(entityDefn));
		table.setDirection(Direction.valueOf(uiOptions.getDirection(entityDefn).toString()));
		table.setShowRowNumbers(uiOptions.getShowRowNumbersValue(entityDefn));
		List<NodeDefinition> childDefinitions = entityDefn.getChildDefinitions();
		for (NodeDefinition childDefn : childDefinitions) {
			TableHeadingComponent component = createTableHeadingComponent(table, childDefn);
			table.addHeadingComponent(component);
		}
		return table;
	}
	
	protected TableHeadingComponent createTableHeadingComponent(TableHeadingContainer parent, NodeDefinition nodeDefn) {
		TableHeadingComponent component;
		if ( nodeDefn instanceof EntityDefinition ) {
			EntityDefinition entityDefn = (EntityDefinition) nodeDefn;
			if ( entityDefn.isMultiple() ) {
				throw new IllegalStateException("Nested multiple entity inside table layout entity is not supported: " + nodeDefn.getPath());
			}
			component = parent.createColumnGroup();
			((ColumnGroup) component).setEntityDefinition(entityDefn);
			
			List<NodeDefinition> innerChildDefns = entityDefn.getChildDefinitions();
			for (NodeDefinition innerChildDefn : innerChildDefns) {
				TableHeadingComponent innerComponent = createTableHeadingComponent(parent, innerChildDefn);
				((ColumnGroup) component).addHeadingComponent(innerComponent);
			}
		} else {
			component = parent.createColumn();
			((Column) component).setAttributeDefinition((AttributeDefinition) nodeDefn);
		}
		return component;
	}

	protected EntityDefinition findAssociatedRootEntity(UITabSet tabSet) {
		UIOptions uiOptions = tabSet.getUIOptions();
		CollectSurvey survey = uiOptions.getSurvey();
		Schema schema = survey.getSchema();
		List<EntityDefinition> rootEntityDefinitions = schema.getRootEntityDefinitions();
		for (EntityDefinition rootEntityDefn : rootEntityDefinitions) {
			UITabSet assignedRootTabSet = uiOptions.getAssignedRootTabSet(rootEntityDefn);
			if ( ObjectUtils.equals(assignedRootTabSet, tabSet) ) {
				return rootEntityDefn;
			}
		}
		return null;
	}

	protected void copyLabels(UITab tab, Form form) {
		List<LanguageSpecificText> labels = tab.getLabels();
		for (LanguageSpecificText lst : labels) {
			form.setLabel(lst.getLanguage(), lst.getText());
		}
	}

	protected void copyLabels(EntityDefinition entityDefn, Form form) {
		List<NodeLabel> labels = getLabelsByType(entityDefn, Type.HEADING);
		for (LanguageSpecificText lst : labels) {
			form.setLabel(lst.getLanguage(), lst.getText());
		}
	}

	protected List<NodeLabel> getLabelsByType(NodeDefinition nodeDefn, NodeLabel.Type type) {
		List<NodeLabel> result = new ArrayList<NodeLabel>();
		List<NodeLabel> labels = nodeDefn.getLabels();
		for (NodeLabel label : labels) {
			if ( label.getType() == type ) {
				result.add(label);
			}
		}
		return result;
	}

}
