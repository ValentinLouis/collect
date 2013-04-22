package org.openforis.collect.ui.component.input {
	import flash.events.ContextMenuEvent;
	import flash.ui.ContextMenu;
	import flash.ui.ContextMenuItem;
	
	import org.openforis.collect.Application;
	import org.openforis.collect.event.EventDispatcherFactory;
	import org.openforis.collect.event.NodeEvent;
	import org.openforis.collect.i18n.Message;
	import org.openforis.collect.metamodel.proxy.AttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.CodeAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.NodeDefinitionProxy;
	import org.openforis.collect.model.CollectRecord$Step;
	import org.openforis.collect.model.proxy.AttributeProxy;
	import org.openforis.collect.model.proxy.EntityProxy;
	import org.openforis.collect.ui.component.detail.AttributeFormItem;
	import org.openforis.collect.ui.component.detail.CollectFormItem;
	import org.openforis.collect.ui.component.detail.TableFormItem;
	import org.openforis.collect.util.CollectionUtil;
	import org.openforis.idm.metamodel.validation.ValidationResultFlag;

	/**
	 * @author M. Togna
	 * @author S. Ricci 
	 * */
	public class FormItemContextMenu {
		
		private static const APPROVE_MISSING:ContextMenuItem = new ContextMenuItem(Message.get("edit.contextMenu.approveMissingValue"), true);
		private static const SET_BLANK_ON_FORM:ContextMenuItem = new ContextMenuItem(Message.get("edit.contextMenu.blankOnForm"), true);
		private static const CONFIRM_ERROR:ContextMenuItem = new ContextMenuItem(Message.get("edit.contextMenu.approveError"), true);
		private static const MENU_ITEMS:Array = [
			APPROVE_MISSING,
			CONFIRM_ERROR,
			SET_BLANK_ON_FORM
		];
		
		private var _contextMenu:ContextMenu;
		private var _formItem:CollectFormItem;

		{
			initEventListeners();
		}
		
		public function FormItemContextMenu(formItem:CollectFormItem) {
			this._contextMenu = new ContextMenu();
			this._contextMenu.hideBuiltInItems();
			this._formItem = formItem;
			updateItems();
			this._formItem.contextMenu = this._contextMenu;
		}
		
		private static function initEventListeners():void {
			//init context menu items' event listener
			var item:ContextMenuItem;
			for each (item in MENU_ITEMS)  {
				item.addEventListener(ContextMenuEvent.MENU_ITEM_SELECT, menuItemSelectHandler);
			}
		}
		
		public function updateItems():void {
			var items:Array = null;
			if(Application.activeRecord != null) {
				var step:CollectRecord$Step = Application.activeRecord.step;
				items = createMenuItems(step);
			}
			_contextMenu.customItems = items;
		}
		
		private function createMenuItems(step:CollectRecord$Step):Array {
			var nodeDefn:NodeDefinitionProxy = _formItem.nodeDefinition;
			var items:Array = new Array();
			if ( nodeDefn != null ) {
				var parentEntity:EntityProxy = _formItem.parentEntity;
				if(parentEntity != null) {
					var nearestParentEntity:EntityProxy = parentEntity.getDescendantNearestParentEntity(nodeDefn);
					var nodeName:String = nodeDefn.name;
					var count:int = nearestParentEntity.getCount(nodeName);
					var minCountValid:ValidationResultFlag = parentEntity.childrenMinCountValidationMap.get(nodeName);
					if(count == 0 || minCountValid == ValidationResultFlag.ERROR) {
						switch(step) {
							/*case CollectRecord$Step.ENTRY:
								items.push(SET_BLANK_ON_FORM);
								break;*/
							case CollectRecord$Step.CLEANSING:
								items.push(APPROVE_MISSING);
								break;
						}
					}
					if(step == CollectRecord$Step.ENTRY) {
						if ( nodeDefn is AttributeDefinitionProxy && ! nodeDefn.multiple ) {
							var attribute:AttributeProxy = nearestParentEntity.getSingleAttribute(nodeDefn.name);
							var hasErrors:Boolean = attribute != null && attribute.hasErrors() &&
								! CollectionUtil.containsItemWith(attribute.validationResults.errors, "ruleName", "SpecifiedValidator");
							if(hasErrors) {
								var hasConfirmedError:Boolean = nearestParentEntity.hasConfirmedError(nodeDefn.name);
								if(! hasConfirmedError) {
									items.push(CONFIRM_ERROR);
								}
							}
						}
					}
				}
			}
			return items;
		}
		
		public static function menuItemSelectHandler(event:ContextMenuEvent):void {
			var formItem:CollectFormItem = event.contextMenuOwner as CollectFormItem;
			var parentEntity:EntityProxy = formItem.parentEntity;
			var nodeDefinition:NodeDefinitionProxy = formItem.nodeDefinition;
			var nearestParentEntity:EntityProxy = parentEntity.getDescendantNearestParentEntity(nodeDefinition);
			var nodeEvent:NodeEvent = null; 
			switch(event.target) {
				case SET_BLANK_ON_FORM:
				case APPROVE_MISSING:
					nodeEvent = new NodeEvent(NodeEvent.APPROVE_MISSING);
					nodeEvent.parentEntity = nearestParentEntity;
					nodeEvent.nodeName = nodeDefinition.name;
					break;
				case CONFIRM_ERROR:
					if(nodeDefinition is AttributeDefinitionProxy) {
						if ( ! nodeDefinition.multiple || nodeDefinition is CodeAttributeDefinitionProxy) {
							nodeEvent = new NodeEvent(NodeEvent.CONFIRM_ERROR);
							nodeEvent.parentEntity = nearestParentEntity;
							nodeEvent.nodeName = nodeDefinition.name;
							
							if ( ! nodeDefinition.multiple) {
								nodeEvent.node = AttributeFormItem(formItem).attribute;
							} else {
								nodeEvent.parentEntity = nearestParentEntity;
								nodeEvent.nodes = nearestParentEntity.getChildren(nodeDefinition.name);
							}
						}
					}
					break;
			}
			if(nodeEvent != null) {
				EventDispatcherFactory.getEventDispatcher().dispatchEvent(nodeEvent);
			}
		}
		
	}
	
}