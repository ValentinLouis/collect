package org.openforis.collect.presenter
{
	import flash.events.FocusEvent;
	import flash.events.MouseEvent;
	
	import mx.binding.utils.ChangeWatcher;
	import mx.collections.ArrayList;
	import mx.collections.IList;
	import mx.events.PropertyChangeEvent;
	import mx.rpc.events.ResultEvent;
	
	import org.openforis.collect.client.ClientFactory;
	import org.openforis.collect.event.ApplicationEvent;
	import org.openforis.collect.event.InputFieldEvent;
	import org.openforis.collect.metamodel.proxy.EntityDefinitionProxy;
	import org.openforis.collect.model.proxy.EntityAddRequestProxy;
	import org.openforis.collect.model.proxy.EntityProxy;
	import org.openforis.collect.model.proxy.NodeDeleteRequestProxy;
	import org.openforis.collect.model.proxy.RecordUpdateRequestSetProxy;
	import org.openforis.collect.model.proxy.RecordUpdateResponseSetProxy;
	import org.openforis.collect.ui.component.detail.FormContainerFormItem;
	import org.openforis.collect.ui.component.input.InputField;
	import org.openforis.collect.util.AlertUtil;
	import org.openforis.collect.util.CollectionUtil;
	import org.openforis.collect.util.UIUtil;
	
	import spark.events.IndexChangeEvent;


	/**
	 * 
	 * @author S. Ricci
	 *  
	 */
	public class FormContainerFormItemPresenter extends FormItemPresenter {
		
		private var _keyTextChangeWatchers:Array;
		private var _addSectionInited:Boolean = false;
		
		public function FormContainerFormItemPresenter(view:FormContainerFormItem) {
			super(view);
		}
		
		override internal function initEventListeners():void {
			super.initEventListeners();
			
			eventDispatcher.addEventListener(InputFieldEvent.VISITED, inputFieldVisitedHandler);
		}

		protected function initAddSection():void {
			view.addSection.addButton.addEventListener(MouseEvent.CLICK, addButtonClickHandler);
			view.addSection.addButton.addEventListener(FocusEvent.FOCUS_IN, buttonFocusInHandler);
			view.addSection.deleteButton.addEventListener(MouseEvent.CLICK, deleteButtonClickHandler);
			view.addSection.deleteButton.addEventListener(FocusEvent.FOCUS_IN, buttonFocusInHandler);
			view.addSection.dropDownList.addEventListener(IndexChangeEvent.CHANGE, dropDownListChangeHandler);
			_addSectionInited = true;
		}
		
		private function get view():FormContainerFormItem {
			return FormContainerFormItem(_view);
		}
		
		protected function buttonFocusInHandler(event:FocusEvent):void {
			UIUtil.ensureElementIsVisible(event.target);
		}
		
		override protected function initValidationDisplayManager():void {
			super.initValidationDisplayManager();
			_validationDisplayManager.showMinMaxCountErrors = true;
		}
		
		override protected function updateView():void {
			if ( view.form.multiple ) {
				view.currentState = FormContainerFormItem.STATE_MULTIPLE;
				if ( ! _addSectionInited ) {
					initAddSection();
				}
			} else {
				view.currentState = FormContainerFormItem.STATE_SINGLE; 
			}
			if(view.form != null && view.parentEntity != null) {
				var entities:IList = getEntities();
				if ( view.form.multiple) {
					view.entities = EntityProxy.sortEntitiesByKey(entities);
					selectEntity(null);
				} else {
					view.entities = null;
					if ( entities.length == 1 ) {
						selectEntity(EntityProxy(entities.getItemAt(0)));
					}
				}
			}
			super.updateView();
		}
		
		protected function getEntities():IList {
			var name:String = view.form.entityDefinition.name;
			var entities:IList = null;
			if ( view.parentEntity != null ) {
				if ( view.parentEntity.definition == view.form.entityDefinition ) {
					entities = new ArrayList();
					entities.addItem(view.parentEntity);
				} else {
					entities = view.parentEntity.getChildren(name);
				}
			}
			return entities;
		}
		
		protected function updateViewEntities():void {
			var selectedEntity:* = view.addSection.dropDownList.selectedItem;
			var entities:IList = getEntities();
			view.entities = EntityProxy.sortEntitiesByKey(entities);
			initEntitiesKeyTextChangeWatchers();
			view.addSection.dropDownList.selectedItem = selectedEntity;
		}
		
		protected function initEntitiesKeyTextChangeWatchers():void {
			for each (var cw:ChangeWatcher in _keyTextChangeWatchers) { 
				cw.unwatch();
			}
			_keyTextChangeWatchers = new Array();
			for each (var entity:EntityProxy in view.entities) {
				var watcher:ChangeWatcher = ChangeWatcher.watch(entity, "keyText", entityKeyTextChangeHandler);
				_keyTextChangeWatchers.push(watcher);
			}
		}
		
		protected function entityKeyTextChangeHandler(event:PropertyChangeEvent):void {
			updateViewEntities();
		}

		protected function addButtonClickHandler(event:MouseEvent):void {
			var entities:IList = getEntities();
			var entityDefn:EntityDefinitionProxy = view.form.entityDefinition;
			var maxCount:Number = entityDefn.maxCount
			if(isNaN(maxCount) || CollectionUtil.isEmpty(entities) || entities.length < maxCount) {
				var r:EntityAddRequestProxy = new EntityAddRequestProxy();
				r.parentEntityId = view.parentEntity.id;
				r.nodeName = entityDefn.name;
				var reqSet:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy(r);
				ClientFactory.dataClient.updateActiveRecord(reqSet, addResultHandler, faultHandler);
			} else {
				var labelText:String = entityDefn.getInstanceOrHeadingLabelText();
				AlertUtil.showError("edit.maxCountExceed", [maxCount, labelText]);
			}
		}
		
		protected function deleteButtonClickHandler(event:MouseEvent):void {
			AlertUtil.showConfirm("global.confirmDelete", [view.form.entityDefinition.getInstanceOrHeadingLabelText()], 
				"global.confirmAlertTitle", performDeletion);
		}
		
		protected function performDeletion():void {
			var r:NodeDeleteRequestProxy = new NodeDeleteRequestProxy();
			r.nodeId = view.selectedEntity.id;
			var reqSet:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy(r);
			ClientFactory.dataClient.updateActiveRecord(reqSet, deleteResultHandler, faultHandler);
		}
		
		protected function inputFieldVisitedHandler(event:InputFieldEvent):void {
			var inputField:InputField = event.inputField;
			if(inputField != null && inputField.parentEntity != null) {
				var entities:IList = getEntities();
				for each (var e:EntityProxy in entities) {
					if(e == inputField.parentEntity) {
						updateValidationDisplayManager();
						break;
					}
				}
			}
		}
		
		override protected function updateValidationDisplayManager():void {
			super.updateValidationDisplayManager();
			if(view.parentEntity != null) {
				var entityDefn:EntityDefinitionProxy = view.form.entityDefinition;
				var name:String = entityDefn.name;
				var visited:Boolean = view.parentEntity.isErrorOnChildVisible(name);
				var active:Boolean = visited;
				if(active) {
					_validationDisplayManager.active = true;
					_validationDisplayManager.displayMinMaxCountValidationErrors(view.parentEntity, entityDefn);
				} else {
					_validationDisplayManager.active = false;
					_validationDisplayManager.reset();
				}
			}
		}
		
		protected function addResultHandler(event:ResultEvent, token:Object = null):void {
			updateViewEntities();
			//select the inserted entity
			_view.callLater(function():void {
				var entities:IList = getEntities();
				var lastEntity:EntityProxy = entities.getItemAt(entities.length -1) as EntityProxy;
				selectEntity(lastEntity, true);
			});
		}
		
		protected function deleteResultHandler(event:ResultEvent, token:Object = null):void {
			var responseSet:RecordUpdateResponseSetProxy = RecordUpdateResponseSetProxy(event.result);
			var appEvt:ApplicationEvent = new ApplicationEvent(ApplicationEvent.UPDATE_RESPONSE_RECEIVED);
			appEvt.result = responseSet;
			eventDispatcher.dispatchEvent(appEvt);
			selectEntity(null);
			updateViewEntities();
		}
		
		protected function dropDownListChangeHandler(event:IndexChangeEvent):void {
			var entity:EntityProxy = view.addSection.dropDownList.selectedItem as EntityProxy;
			selectEntity(entity);
		}
		
		protected function selectEntity(entity:EntityProxy, resetView:Boolean = false):void {
			if ( view.currentState == FormContainerFormItem.STATE_MULTIPLE ) {
				view.addSection.dropDownList.selectedItem = entity;
			}
			view.internalContainer.reset();
			view.selectedEntity = entity;
			if(entity != null) {
				showInternalContainer();
			} else {
				hideInternalContainer();
			}
		}
		
		protected function showInternalContainer():void {
			if(view.internalContainer.visible) {
				//internal container already visible, call programmatically the showEffect
				view.showFormEffect.play([view.internalContainer]);
			} else {
				view.internalContainer.visible = true;
			}
		}
		
		protected function hideInternalContainer():void {
			view.internalContainer.visible = false;
		}
		
	}
}