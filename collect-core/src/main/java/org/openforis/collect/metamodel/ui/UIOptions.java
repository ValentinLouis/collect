package org.openforis.collect.metamodel.ui;

import static org.openforis.collect.metamodel.ui.UIOptionsConstants.UI_NAMESPACE_URI;
import static org.openforis.collect.metamodel.ui.UIOptionsConstants.UI_TYPE;
import static org.openforis.idm.metamodel.TaxonAttributeDefinition.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.commons.collection.CollectionUtils;
import org.openforis.idm.metamodel.ApplicationOptions;
import org.openforis.idm.metamodel.AttributeDefinition;
import org.openforis.idm.metamodel.CodeAttributeDefinition;
import org.openforis.idm.metamodel.CoordinateAttributeDefinition;
import org.openforis.idm.metamodel.EntityDefinition;
import org.openforis.idm.metamodel.LanguageSpecificText;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.metamodel.NodeDefinitionVisitor;
import org.openforis.idm.metamodel.NodeLabel;
import org.openforis.idm.metamodel.Schema;
import org.openforis.idm.metamodel.TextAttributeDefinition;


/**
 * @author M. Togna
 * @author S. Ricci
 * 
 */
@Deprecated
public class UIOptions implements ApplicationOptions, Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final String VISIBLE_FIELDS_SEPARATOR = ",";
	
	private static final String TABSET_NAME_PREFIX = "tabset_";
	private static final String TAB_NAME_PREFIX = "tab_";

	public static final List<String[]> TAXON_VISIBLE_FIELDS_TEMPLATES = Arrays.asList(
		new String[] {
				CODE_FIELD_NAME,
				SCIENTIFIC_NAME_FIELD_NAME,
				VERNACULAR_NAME_FIELD_NAME
		}
		,
		new String[] {
				CODE_FIELD_NAME,
				SCIENTIFIC_NAME_FIELD_NAME
		}
		,
		new String[] {
				CODE_FIELD_NAME,
				VERNACULAR_NAME_FIELD_NAME
		}
		,
		new String[] {
				VERNACULAR_NAME_FIELD_NAME
		}
	);
	
	public enum Annotation {
		TAB_SET(new QName(UI_NAMESPACE_URI, UIOptionsConstants.TAB_SET_NAME)),
		TAB_NAME(new QName(UI_NAMESPACE_URI, UIOptionsConstants.TAB)),
		LAYOUT(new QName(UI_NAMESPACE_URI, UIOptionsConstants.LAYOUT)),
		DIRECTION(new QName(UI_NAMESPACE_URI, UIOptionsConstants.DIRECTION)),
		COUNT_IN_SUMMARY_LIST(new QName(UI_NAMESPACE_URI, UIOptionsConstants.COUNT)),
		SHOW_ROW_NUMBERS(new QName(UI_NAMESPACE_URI, UIOptionsConstants.SHOW_ROW_NUMBERS)),
		AUTOCOMPLETE(new QName(UI_NAMESPACE_URI, UIOptionsConstants.AUTOCOMPLETE)),
		FIELDS_ORDER(new QName(UI_NAMESPACE_URI, UIOptionsConstants.FIELDS_ORDER)),
		VISIBLE_FIELDS(new QName(UI_NAMESPACE_URI, UIOptionsConstants.VISIBLE_FIELDS)),
		SHOW_ALLOWED_VALUES_PREVIEW(new QName(UI_NAMESPACE_URI, UIOptionsConstants.SHOW_ALLOWED_VALUES_PREVIEW));
		
		private QName qName;

		private Annotation(QName qname) {
			this.qName = qname;
		}
		
		public QName getQName() {
			return qName;
		}
	}
	
	public enum CoordinateAttributeFieldsOrder {
		
		SRS_X_Y("srs_x_y"), 
		SRS_Y_X("srs_y_x");
		
		public static final CoordinateAttributeFieldsOrder DEFAULT = SRS_Y_X;
		
		private String value;

		private CoordinateAttributeFieldsOrder(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
	}
	
	public enum Layout {
		FORM, TABLE
	}
	
	public enum Direction {
		BY_ROWS("byRows"), 
		BY_COLUMNS("byColumns");
		
		private String value;

		private Direction(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	private CollectSurvey survey;
	private List<UITabSet> tabSets;
	
	public UIOptions(CollectSurvey survey) {
		this.survey = survey;
	}

	@Override
	public String getType() {
		return UI_TYPE;
	}

	public CollectSurvey getSurvey() {
		return survey;
	}
	
	public void setSurvey(CollectSurvey survey) {
		this.survey = survey;
	}
	
	public List<UITabSet> getTabSets() {
		return CollectionUtils.unmodifiableList(tabSets);
	}
	
	public UITabSet createTabSet() {
		return createTabSet(TABSET_NAME_PREFIX + survey.nextId());
	}
	
	public UITabSet createTabSet(String name) {
		UITabSet result = new UITabSet(this);
		result.setName(name);
		return result;
	}
	
	public UITab createTab() {
		return createTab(TAB_NAME_PREFIX + survey.nextId());
	}

	public UITab createTab(String name) {
		UITab result = new UITab(this);
		result.setName(name);
		return result;
	}
	
	public UITabSet createRootTabSet(EntityDefinition rootEntity) {
		UIOptions uiOpts = survey.getUIOptions();
		UITabSet tabSet = uiOpts.createTabSet();
		UITab mainTab = createMainTab(rootEntity, tabSet);
		uiOpts.addTabSet(tabSet);
		uiOpts.assignToTabSet(rootEntity, tabSet);
		uiOpts.assignToTab(rootEntity, mainTab);
		return tabSet;
	}

	protected UITab createMainTab(EntityDefinition nodeDefn,
			UITabSet tabSet) {
		UITab tab = createTab();
		copyLabels(nodeDefn, tab);
		tabSet.addTab(tab);
		return tab;
	}

	protected void copyLabels(EntityDefinition nodeDefn, UITab tab) {
		removeLabels(tab);
		List<NodeLabel> labels = nodeDefn.getLabels();
		for (NodeLabel label : labels) {
			if ( label.getType() == NodeLabel.Type.INSTANCE ) {
				tab.setLabel(label.getLanguage(), label.getText());
			}
		}
	}

	protected void removeLabels(UITab tab) {
		List<LanguageSpecificText> labels = tab.getLabels();
		for (LanguageSpecificText label : labels) {
			tab.removeLabel(label.getLanguage());
		}
	}
	
	public UITab getMainTab(UITabSet rootTabSet) {
		List<UITab> tabs = rootTabSet.getTabs();
		if ( tabs.isEmpty() ) {
			return null;
		} else {
			return tabs.get(0);
		}
	}
	
	public boolean isMainTab(UITab tab) {
		return tab.getIndex() == 0 && tab.getDepth() == 1;
	}
	
	public UITab getTab(String name) {
		Stack<UITab> stack = new Stack<UITab>();
		List<UITabSet> tabSets = getTabSets();
		for (UITabSet tabSet : tabSets) {
			stack.addAll(tabSet.getTabs());
			while ( ! stack.isEmpty() ) {
				UITab tab = stack.pop();
				if ( name.equals(tab.getName()) ) {
					return tab;
				}
				stack.addAll(tab.getTabs());
			}
		}
		return null;
	}
	
	public UITab getAssignedTab(NodeDefinition nodeDefn) {
		return getAssignedTab(nodeDefn, true);
	}
	
	public UITab getAssignedTab(NodeDefinition nodeDefn, boolean includeInherited) {
		EntityDefinition parentDefn = (EntityDefinition) nodeDefn.getParentDefinition();
		return getAssignedTab(parentDefn, nodeDefn, includeInherited);
	}
	
	public UITab getAssignedTab(EntityDefinition parentDefn, NodeDefinition nodeDefn, boolean includeInherited) {
		UITab result = null;
		UITabSet rootTabSet = getAssignedRootTabSet(parentDefn, nodeDefn);
		if ( rootTabSet != null ) {
			String tabName = nodeDefn.getAnnotation(Annotation.TAB_NAME.getQName());
			if ( StringUtils.isNotBlank(tabName) ) {
				if ( parentDefn == null || parentDefn.getParentDefinition() == null ) {
					result = rootTabSet.getTab(tabName);
					if ( result == null ) {
						//try to find tab among main tab children
						UITab mainTab = getMainTab(rootTabSet);
						result = mainTab.getTab(tabName);
					}
				} else {
					UITab parentTab = getAssignedTab(parentDefn);
					if ( parentTab != null ) {
						if ( tabName.equals(parentTab.getName()) ) {
							result = parentTab;
						} else {
							result = parentTab.getTab(tabName);
						}
					}
				}
			} else {
				if ( includeInherited ) {
					UITab parentTab = getAssignedTab(parentDefn);
					result = parentTab;
				}
			}
		}
		return result;
	}

	protected UITabSet getAssignedRootTabSet(EntityDefinition parentDefn, NodeDefinition nodeDefn) {
		EntityDefinition rootEntityDefn;
		if ( parentDefn != null ) {
			rootEntityDefn = parentDefn.getRootEntity();
		} else if ( nodeDefn instanceof EntityDefinition ) {
			rootEntityDefn = (EntityDefinition) nodeDefn;
		} else {
			throw new IllegalArgumentException("Parent entity definition not specified");
		}
		UITabSet tabSet = getAssignedRootTabSet(rootEntityDefn);
		return tabSet;
	}

	public UITabSet getAssignedRootTabSet(EntityDefinition rootEntityDefn) {
		String tabSetName = rootEntityDefn.getAnnotation(Annotation.TAB_SET.getQName());
		UITabSet tabSet = getTabSet(tabSetName);
		return tabSet;
	}
	
	public List<UITab> getAssignableTabs(EntityDefinition parentEntity, NodeDefinition contextNode) {
		boolean contextNodeIsMultipleEntity = contextNode instanceof EntityDefinition && contextNode.isMultiple();
		Layout contextNodeLayout = contextNode instanceof EntityDefinition ? getLayout((EntityDefinition) contextNode): null;
		int contextNodeId = contextNode.getId();
		return getAssignableTabs(parentEntity, contextNodeIsMultipleEntity,
				contextNodeLayout, contextNodeId);
	}

	public List<UITab> getAssignableTabs(EntityDefinition parentEntity,
			boolean contextNodeIsMultipleEntity, Layout contextNodeLayout,
			int contextNodeId) {
		List<UITab> result = new ArrayList<UITab>(getTabsAssignableToChildren(parentEntity));
		Iterator<UITab> it = result.iterator();
		while (it.hasNext()) {
			UITab tab = (UITab) it.next();
			boolean mainTab = isMainTab(tab);
			EntityDefinition associatedMultipleEntityForm = getFormLayoutMultipleEntity(tab);
			if ( mainTab && contextNodeIsMultipleEntity && 
					contextNodeLayout == Layout.FORM ||
					associatedMultipleEntityForm != null && associatedMultipleEntityForm.getId() != contextNodeId ) {
				it.remove();
			}
		}
		return result;
	}
	
	public UITabSet getAssignedTabSet(EntityDefinition entityDefn) {
		if ( entityDefn.getParentDefinition() == null ) {
			return getAssignedRootTabSet(entityDefn);
		} else {
			return getAssignedTab(entityDefn);
		}
	}

	public List<UITab> getTabsAssignableToChildren(EntityDefinition entityDefn) {
		return getTabsAssignableToChildren(entityDefn, true);
	}
	
	public List<UITab> getTabsAssignableToChildren(EntityDefinition entityDefn, boolean includeAlreadyAssignedTabs) {
		EntityDefinition rootEntity = entityDefn.getRootEntity();
		UITabSet rootTabSet = getAssignedRootTabSet(rootEntity);
		if ( rootTabSet == null || getLayout(entityDefn) == Layout.TABLE) {
			return Collections.emptyList();
		} else if ( entityDefn == rootEntity ) {
			List<UITab> tabs = new ArrayList<UITab>(rootTabSet.getTabs());
			UITab mainTab = getMainTab(rootTabSet);
			tabs.addAll(mainTab.getTabs());
			return tabs;
		} else {
			UITab assignedTab = getAssignedTab(entityDefn);
			List<UITab> childTabs = assignedTab.getTabs();
			List<UITab> validTabs;
			if ( includeAlreadyAssignedTabs ) {
				validTabs = childTabs;
			} else {
				validTabs = new ArrayList<UITab>();
				for (UITab childTab : childTabs) {
					boolean valid = true;
					List<NodeDefinition> nodesPerTab = getNodesPerTab(childTab, false);
					for (NodeDefinition node : nodesPerTab) {
						if ( ! node.isDescendantOf(entityDefn) ) {
							valid = false;
							break;
						}
					}
					if ( valid ) {
						validTabs.add(childTab);
					}
				}
			}
			return validTabs;
		}
	}
	
	public boolean isAssignableTo(NodeDefinition nodeDefn, UITab tab) {
		EntityDefinition parentEntityDefn = (EntityDefinition) nodeDefn.getParentDefinition();
		List<UITab> allowedTabs = getTabsAssignableToChildren(parentEntityDefn);
		boolean result = allowedTabs.contains(tab);
		return result;
	}
	
	/**
	 * Returns true if the specified tab is not assigned to one of the descendants of the belonging rootEntity
	 */
	public boolean isUnassigned(UITab tab) {
		UITabSet rootTabSet = tab.getRootTabSet();
		EntityDefinition rootEntityDefinition = getRootEntityDefinition(rootTabSet);
		return isUnassigned(tab, rootEntityDefinition);
	}
	/**
	 * Returns true if the specified tab is not assigned to one of the descendants of the specified rootEntity
	 */
	public boolean isUnassigned(UITab tab, EntityDefinition rootEntity) {
		Stack<NodeDefinition> stack = new Stack<NodeDefinition>();
		stack.add(rootEntity);
		while ( ! stack.isEmpty() ) {
			NodeDefinition childDefn = stack.pop();
			UITab assignedTab = getAssignedTab(childDefn, false);
			if ( tab == assignedTab ) {
				return false;
			}
			if ( childDefn instanceof EntityDefinition ) {
				List<NodeDefinition> nestedChildDefns = ((EntityDefinition) childDefn).getChildDefinitions();
				for (NodeDefinition nestedChildDefn : nestedChildDefns) {
					stack.push(nestedChildDefn);
				}
			}
		}
		return true;
	}
	
	public void removeUnassignedTabs() {
		List<UITabSet> rootTabSets = getTabSets();
		for (UITabSet rootTabSet : rootTabSets) {
			Stack<UITabSet> stack = new Stack<UITabSet>();
			stack.push(rootTabSet);
			while ( ! stack.isEmpty() ) {
				UITabSet tabSet = stack.pop();
				List<UITab> childTabs = tabSet.getTabs();
				if ( childTabs.isEmpty() ) {
					//leaf, remove it and all ancestors if leaf and unassigned
					if ( tabSet instanceof UITab ) {
						UITab leaf = (UITab) tabSet;
						while ( leaf.getParent() != null && isUnassigned(leaf) ) {
							UITabSet parent = leaf.getParent();
							parent.removeTab(leaf);
							if ( parent instanceof UITab && parent.getTabs().isEmpty() ) {
								leaf = (UITab) parent;
							} else {
								break;
							}
						}
					}
				} else {
					for (UITab childTab : tabSet.getTabs()) {
						stack.push(childTab);
					}
				}
			}
		}
	}

	public EntityDefinition getParentEntityForAssignedNodes(final UITab tab) {
		UITabSet root = tab.getRootTabSet();
		EntityDefinition rootEntity = getRootEntityDefinition(root);
		Stack<NodeDefinition> stack = new Stack<NodeDefinition>();
		stack.push(rootEntity);
		while ( ! stack.isEmpty() ) {
			NodeDefinition nodeDefn = stack.pop();
			if ( nodeDefn instanceof EntityDefinition ) {
				EntityDefinition entityDefn = (EntityDefinition) nodeDefn;
				UITab assignedTab = getAssignedTab(entityDefn, false);
				if ( assignedTab == tab ) {
					return entityDefn;
				}
				List<NodeDefinition> children = entityDefn.getChildDefinitions();
				for (NodeDefinition child : children) {
					stack.push(child);
				}
			}
		}
		throw new IllegalStateException("Parent entity for assigned nodes not found for tab: " + tab.getName());
	}
	
	public boolean isAssociatedWithMultipleEntityForm(UITab tab) {
		return getFormLayoutMultipleEntity(tab) != null;
	}
	
	public List<NodeDefinition> getNodesPerTab(UITab tab, boolean includeDescendants) {
		List<NodeDefinition> result = new ArrayList<NodeDefinition>();
		UITabSet tabSet = tab.getRootTabSet();
		EntityDefinition rootEntity = getRootEntityDefinition(tabSet);
		Queue<NodeDefinition> queue = new LinkedList<NodeDefinition>();
		queue.addAll(rootEntity.getChildDefinitions());
		while ( ! queue.isEmpty() ) {
			NodeDefinition defn = queue.remove();
			UITab nodeTab = getAssignedTab(defn);
			boolean nodeInTab = false;
			if ( nodeTab != null && nodeTab.equals(tab) ) {
				result.add(defn);
				nodeInTab = true;
			}
			if ( defn instanceof EntityDefinition && (includeDescendants || !nodeInTab) ) {
				queue.addAll(((EntityDefinition) defn).getChildDefinitions());
			}
		}
		return result;
	}

	public void assignToTabSet(EntityDefinition rootEntity, UITabSet tabSet) {
		String name = tabSet.getName();
		rootEntity.setAnnotation(Annotation.TAB_SET.getQName(), name);
	}

	public void assignToTab(NodeDefinition nodeDefn, UITab tab) {
		String tabName = tab.getName();
		nodeDefn.setAnnotation(Annotation.TAB_NAME.getQName(), tabName);
		
		afterTabAssociationChanged(nodeDefn);
	}

	protected void afterTabAssociationChanged(NodeDefinition nodeDefn) {
		if ( nodeDefn instanceof EntityDefinition && nodeDefn.getParentDefinition() != null ) {
			removeInvalidTabAssociationInDescendants((EntityDefinition) nodeDefn);
		}
	}

	protected void removeInvalidTabAssociationInDescendants(EntityDefinition entityDefn) {
		entityDefn.traverse(new NodeDefinitionVisitor() {
			@Override
			public void visit(NodeDefinition descendantDefn) {
				UITab descendantTab = getAssignedTab(descendantDefn, false);
				if ( descendantTab == null ) {
					//tab not set or not found
					performTabAssociationRemoval(descendantDefn);
				}
			}
		});
	}
	
	public void removeTabAssociation(NodeDefinition nodeDefn) {
		performTabAssociationRemoval(nodeDefn);
		
		afterTabAssociationChanged(nodeDefn);
	}

	protected void performTabAssociationRemoval(NodeDefinition nodeDefn) {
		nodeDefn.setAnnotation(Annotation.TAB_NAME.getQName(), null);
	}
	
	public void removeTabAssociation(UITab tab) {
		Stack<UITab> stack = new Stack<UITab>();
		stack.push(tab);
		while ( ! stack.isEmpty() ) {
			UITab currentTab = stack.pop();
			List<NodeDefinition> nodesPerTab = getNodesPerTab(currentTab, true);
			for (NodeDefinition nodeDefn : nodesPerTab) {
				performTabAssociationRemoval(nodeDefn);
			}
			List<UITab> childTabs = currentTab.getTabs();
			stack.addAll(childTabs);
		}
	}

	public boolean isAssociatedToTab(NodeDefinition nodeDefn) {
		UITab tab = getAssignedTab(nodeDefn);
		return tab != null;
	}
	
	public Layout getLayout(EntityDefinition node) {
		String layoutValue = node.getAnnotation(Annotation.LAYOUT.getQName());
		if ( layoutValue == null ) {
			EntityDefinition parent = (EntityDefinition) node.getParentDefinition();
			if ( parent != null ) {
				if ( node.isMultiple()) {
					return Layout.TABLE;
				} else {
					return getLayout(parent);
				}
			} else {
				return Layout.FORM;
			}
		}
		return layoutValue != null ? Layout.valueOf(layoutValue.toUpperCase()): null;
	}

	public void setLayout(EntityDefinition entityDefn, Layout layout) {
		String layoutValue = layout != null ? layout.name().toLowerCase(): null;
		entityDefn.setAnnotation(Annotation.LAYOUT.getQName(), layoutValue);
	}
	
	public Direction getDirection(EntityDefinition defn) {
		String value = defn.getAnnotation(Annotation.DIRECTION.getQName());
		if ( value == null ) {
			EntityDefinition parentDefn = (EntityDefinition) defn.getParentDefinition();
			if ( parentDefn == null ) {
				return Direction.BY_ROWS;
			} else {
				return getDirection(parentDefn);
			}
		} else if ( value.equals(Direction.BY_COLUMNS.getValue())) {
			return Direction.BY_COLUMNS;
		} else {
			return Direction.BY_ROWS;
		}
	}
	
	public void setDirection(EntityDefinition defn, Direction direction) {
		String value = direction == null ? null: direction.getValue();
		defn.setAnnotation(Annotation.DIRECTION.getQName(), value);
	}
	
	public CoordinateAttributeFieldsOrder getFieldsOrder(CoordinateAttributeDefinition defn) {
		String value = defn.getAnnotation(Annotation.FIELDS_ORDER.getQName());
		if ( value == null ) {
			return CoordinateAttributeFieldsOrder.DEFAULT;
		} else {
			return CoordinateAttributeFieldsOrder.valueOf(value.toUpperCase());
		}
	}
	
	public void setFieldsOrder(CoordinateAttributeDefinition defn, CoordinateAttributeFieldsOrder fieldsOrder) {
		String value;
		if ( fieldsOrder == null || fieldsOrder == CoordinateAttributeFieldsOrder.DEFAULT ) {
			value = null;
		} else {
			value = fieldsOrder.name().toLowerCase();
		}
		defn.setAnnotation(Annotation.FIELDS_ORDER.getQName(), value);
	}
	
	public boolean isVisibleField(AttributeDefinition defn, String field) {
		String[] visibleFields = getVisibleFields(defn);
		return Arrays.asList(visibleFields).contains(field);
	}
	
	public String[] getVisibleFields(AttributeDefinition defn) {
		String value = defn.getAnnotation(Annotation.VISIBLE_FIELDS.getQName());
		if ( StringUtils.isBlank(value) ) {
			//when no annotation is specified, all fields are visible
			return defn.getFieldNames().toArray(new String[0]);
		} else {
			String[] fields = value.split(VISIBLE_FIELDS_SEPARATOR);
			return fields;
		}
	}
	
	public void setVisibleFields(AttributeDefinition defn, String[] fields) {
		String value;
		if ( fields == null || fields.length == defn.getFieldNames().size() ) {
			//do not store annotation value when all fields are visible
			value = null;
		} else {
			value = StringUtils.join(fields, VISIBLE_FIELDS_SEPARATOR);
		}
		defn.setAnnotation(Annotation.VISIBLE_FIELDS.getQName(), value);
	}
	
	public boolean getShowRowNumbersValue(EntityDefinition defn) {
		String annotationValue = defn.getAnnotation(Annotation.SHOW_ROW_NUMBERS.getQName());
		return Boolean.valueOf(annotationValue);
	}
	
	public void setShowRowNumbersValue(EntityDefinition defn, boolean value) {
		defn.setAnnotation(Annotation.SHOW_ROW_NUMBERS.getQName(), Boolean.toString(value));
	}
	
	public boolean getCountInSumamryListValue(EntityDefinition defn) {
		String annotationValue = defn.getAnnotation(Annotation.COUNT_IN_SUMMARY_LIST.getQName());
		return Boolean.valueOf(annotationValue);
	}
	
	public void setCountInSummaryListValue(EntityDefinition defn, boolean value) {
		defn.setAnnotation(Annotation.COUNT_IN_SUMMARY_LIST.getQName(), Boolean.toString(value));
	}
	
	public String getAutoCompleteGroup(TextAttributeDefinition defn) {
		String annotationValue = defn.getAnnotation(Annotation.AUTOCOMPLETE.getQName());
		return annotationValue;
	}
	
	public void setAutoCompleteGroup(TextAttributeDefinition defn, String value) {
		defn.setAnnotation(Annotation.AUTOCOMPLETE.getQName(), value);
	}
	
	public boolean getShowAllowedValuesPreviewValue(CodeAttributeDefinition defn) {
		String annotationValue = defn.getAnnotation(Annotation.SHOW_ALLOWED_VALUES_PREVIEW.getQName());
		return Boolean.valueOf(annotationValue);
	}
	
	public void setShowAllowedValuesPreviewValue(CodeAttributeDefinition defn, boolean value) {
		defn.setAnnotation(Annotation.SHOW_ALLOWED_VALUES_PREVIEW.getQName(), value ? Boolean.toString(value): null);
	}
	
	/**
	 * Supported layouts are:
	 * - Root entity: FORM
	 * - Single entity: FORM and TABLE (only inside an entity with TABLE layout)
	 * - Multiple entity: FORM only if it's the only multiple entity with FORM layout
	 * 		into the tab, TABLE only if there is not an ancestor entity with TABLE layout
	 * 
	 * @param parentEntityDefn
	 * @param entityDefn
	 * @param layout
	 * @return 
	 */
	public boolean isLayoutSupported(EntityDefinition parentEntityDefn, int entityDefnId, UITab associatedTab, boolean multiple, Layout layout) {
		if ( parentEntityDefn == null ) {
			return layout == Layout.FORM;
		} else if ( ! multiple ) {
			Layout parentLayout = getLayout(parentEntityDefn);
			return layout == Layout.FORM || parentLayout == Layout.TABLE;
		} else if ( layout == Layout.FORM) {
			UITab tab = associatedTab == null ? getAssignedTab(parentEntityDefn, true): associatedTab;
			EntityDefinition multipleEntity = getFormLayoutMultipleEntity(tab);
			return multipleEntity == null || multipleEntity.getId() == entityDefnId;
		} else {
			EntityDefinition ancestorEntity = parentEntityDefn;
			while ( ancestorEntity != null ) {
				if ( getLayout(ancestorEntity) == Layout.TABLE) {
					return false;
				}
				ancestorEntity = (EntityDefinition) ancestorEntity.getParentDefinition();
			}
			return true;
		}
	}

	protected EntityDefinition getFormLayoutMultipleEntity(UITab tab) {
		List<NodeDefinition> nodesPerTab = getNodesPerTab(tab, false);
		for (NodeDefinition nodeDefn : nodesPerTab) {
			if ( nodeDefn instanceof EntityDefinition && nodeDefn.isMultiple() ) {
				Layout nodeLayout = getLayout((EntityDefinition) nodeDefn);
				if ( nodeLayout == Layout.FORM ) {
					return (EntityDefinition) nodeDefn;
				}
			}
		}
		return null;
	}

	public EntityDefinition getRootEntityDefinition(UITabSet tabSet) {
		Schema schema = survey.getSchema();
		List<EntityDefinition> rootEntityDefinitions = schema.getRootEntityDefinitions();
		for (EntityDefinition defn : rootEntityDefinitions) {
			UITabSet entityTabSet = getAssignedRootTabSet(defn);
			if ( entityTabSet != null && entityTabSet.equals(tabSet) ) {
				return defn;
			}
		}
		return null;
	}
	
	public UITabSet getTabSet(String name) {
		if ( tabSets != null ) {
			for (UITabSet tabSet : tabSets) {
				if ( tabSet.getName().equals(name) ) {
					return tabSet;
				}
			}
		}
		return null;
	}
	
	public void addTabSet(UITabSet tabSet) {
		if ( tabSets == null ) {
			tabSets = new ArrayList<UITabSet>();
		}
		tabSets.add(tabSet);
	}
	
	public void setTabSet(int index, UITabSet tabSet) {
		if ( tabSets == null ) {
			tabSets = new ArrayList<UITabSet>();
		}
		tabSets.set(index, tabSet);
	}
	
	public void removeTabSet(UITabSet tabSet) {
		tabSets.remove(tabSet);
	}
	
	public UITabSet updateTabSet(String name, String newName) {
		UITabSet tabSet = getTabSet(name);
		tabSet.setName(newName);
		return tabSet;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tabSets == null) ? 0 : tabSets.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UIOptions other = (UIOptions) obj;
		if (tabSets == null) {
			if (other.tabSets != null)
				return false;
		} else if (!tabSets.equals(other.tabSets))
			return false;
		return true;
	}

}
