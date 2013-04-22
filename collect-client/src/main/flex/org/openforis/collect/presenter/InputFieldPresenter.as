package org.openforis.collect.presenter {
	import flash.events.Event;
	import flash.events.FocusEvent;
	import flash.events.KeyboardEvent;
	import flash.ui.Keyboard;
	
	import mx.binding.utils.BindingUtils;
	import mx.collections.ArrayCollection;
	import mx.collections.IList;
	import mx.rpc.events.ResultEvent;
	
	import org.openforis.collect.Application;
	import org.openforis.collect.client.ClientFactory;
	import org.openforis.collect.client.DataClient;
	import org.openforis.collect.event.ApplicationEvent;
	import org.openforis.collect.event.InputFieldEvent;
	import org.openforis.collect.event.NodeEvent;
	import org.openforis.collect.metamodel.proxy.AttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.CodeAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.DateAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.NumberAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.RangeAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.ui.UIOptions$Direction;
	import org.openforis.collect.metamodel.ui.proxy.ColumnProxy;
	import org.openforis.collect.model.FieldSymbol;
	import org.openforis.collect.model.proxy.AttributeAddRequestProxy;
	import org.openforis.collect.model.proxy.AttributeProxy;
	import org.openforis.collect.model.proxy.AttributeUpdateRequestProxy;
	import org.openforis.collect.model.proxy.AttributeUpdateResponseProxy;
	import org.openforis.collect.model.proxy.CodeAttributeProxy;
	import org.openforis.collect.model.proxy.ConfirmErrorRequestProxy;
	import org.openforis.collect.model.proxy.DefaultValueApplyRequestProxy;
	import org.openforis.collect.model.proxy.EntityProxy;
	import org.openforis.collect.model.proxy.FieldProxy;
	import org.openforis.collect.model.proxy.FieldUpdateRequestProxy;
	import org.openforis.collect.model.proxy.MissingValueApproveRequestProxy;
	import org.openforis.collect.model.proxy.NodeDeleteRequestProxy;
	import org.openforis.collect.model.proxy.NodeProxy;
	import org.openforis.collect.model.proxy.NodeUpdateResponseProxy;
	import org.openforis.collect.model.proxy.RecordUpdateRequestProxy;
	import org.openforis.collect.model.proxy.RecordUpdateRequestSetProxy;
	import org.openforis.collect.model.proxy.RecordUpdateResponseSetProxy;
	import org.openforis.collect.model.proxy.RemarksUpdateRequestProxy;
	import org.openforis.collect.ui.component.input.InputField;
	import org.openforis.collect.ui.component.input.InputFieldContextMenu;
	import org.openforis.collect.util.StringUtil;
	import org.openforis.collect.util.UIUtil;
	
	/**
	 * 
	 * @author M. Togna
	 * @author S. Ricci
	 * */
	public class InputFieldPresenter extends AbstractPresenter {
		
		private var _view:InputField;
		private var _contextMenu:InputFieldContextMenu;
		
		private static var _dataClient:DataClient;
		
		/*
			Static initialization
		*/
		{
			_dataClient = ClientFactory.dataClient;
			eventDispatcher.addEventListener(NodeEvent.UPDATE_SYMBOL, updateSymbolHandler);
			eventDispatcher.addEventListener(NodeEvent.UPDATE_REMARKS, updateRemarksHandler);
			eventDispatcher.addEventListener(NodeEvent.DELETE_NODE, deleteNodeHandler);
			eventDispatcher.addEventListener(NodeEvent.CONFIRM_ERROR, confirmErrorHandler);
			eventDispatcher.addEventListener(NodeEvent.APPROVE_MISSING, approveMissingHandler);
			eventDispatcher.addEventListener(NodeEvent.APPLY_DEFAULT_VALUE, applyDefaultValueHandler);
		}
		
		public function InputFieldPresenter(inputField:InputField) {
			_view = inputField;
			_contextMenu = new InputFieldContextMenu(_view);
			super();
			//updateView();
		}
		
		override internal function initEventListeners():void {
			super.initEventListeners();
			
			eventDispatcher.addEventListener(ApplicationEvent.UPDATE_RESPONSE_RECEIVED, updateResponseReceivedHandler);
			eventDispatcher.addEventListener(InputFieldEvent.SET_FOCUS, setFocusHandler);
			
			if(_view.textInput != null) {
				_view.textInput.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
				_view.textInput.addEventListener(Event.CHANGE, changeHandler);
				_view.textInput.addEventListener(FocusEvent.FOCUS_OUT, focusOutHandler);
				_view.textInput.addEventListener(FocusEvent.FOCUS_IN, focusInHandler);
			}
			_view.addEventListener(FocusEvent.KEY_FOCUS_CHANGE, keyFocusChangeHandler);
			
			BindingUtils.bindSetter(setAttribute, _view, "attribute");
		}
		
		protected function setFocusHandler(event:InputFieldEvent):void {
			if ( _view.textInput != null && _view.attribute != null && 
					_view.attribute.id == event.attributeId && 
					_view.fieldIndex == event.fieldIdx ) {
				_view.textInput.setFocus();
			}
		}
		
		protected static function approveMissingHandler(event:NodeEvent): void {
			var reqSet:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy();
			if(event.node != null || event.nodes != null) {
				var node:NodeProxy = event.node;
				if(node == null) {
					var nodes:IList = event.nodes;
					if(nodes != null && nodes.length == 1) {
						//approve only first node, if it's a missing value it should be an empty node
						node = nodes.getItemAt(0) as NodeProxy;
					}
				}
				prepareApproveMissingRequests(reqSet, node, event.fieldIdx, event.applyToNonEmptyNodes);
			} else {
				var r:MissingValueApproveRequestProxy = new MissingValueApproveRequestProxy();
				r.parentEntityId = event.parentEntity.id;
				r.nodeName = event.nodeName;
				reqSet.addRequest(r);
			}
			_dataClient.updateActiveRecord(reqSet, null, faultHandler);
		}
		
		protected static function applyDefaultValueHandler(event:NodeEvent):void {
			var node:NodeProxy = event.node;
			var r:DefaultValueApplyRequestProxy = new DefaultValueApplyRequestProxy();
			r.nodeId = node.id;
			
			var reqSet:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy(r);
			_dataClient.updateActiveRecord(reqSet, null, faultHandler);
		}
		
		protected static function updateRemarksHandler(event:NodeEvent): void {
			var reqSet:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy();
			var fieldIdx:int;
			if(event.node != null) {
				var attribute:AttributeProxy = AttributeProxy(event.node);
				if(event.fieldIdx >= 0) {
					prepareUpdateRemarksRequest(reqSet, attribute, event.remarks, event.fieldIdx);
				} else {
					for (fieldIdx = 0; fieldIdx < attribute.fields.length; fieldIdx++) {
						prepareUpdateRemarksRequest(reqSet, attribute, event.remarks, fieldIdx);
					}
				}
			} else {
				//considering nodes as attributes
				var attributes:IList = event.nodes;
				for each (attribute in attributes) {
					for (fieldIdx = 0; fieldIdx < attribute.fields.length; fieldIdx++) {
						prepareUpdateRemarksRequest(reqSet, attribute, event.remarks, fieldIdx);
					}
				}
			}
			_dataClient.updateActiveRecord(reqSet, null, faultHandler);
		}
		
		protected static function prepareUpdateRemarksRequest(reqSet:RecordUpdateRequestSetProxy, node:NodeProxy, remarks:String, fieldIdx:Number = NaN):void {
			var r:RemarksUpdateRequestProxy = new RemarksUpdateRequestProxy();
			r.nodeId = node.id;
			r.fieldIndex = fieldIdx;
			r.remarks = remarks;
			reqSet.addRequest(r);
		}
		
		protected static function updateSymbolHandler(event:NodeEvent): void {
			var updRequestSet:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy();
			prepareUpdateSymbolRequests(updRequestSet, event.node, event.symbol, event.fieldIdx, event.applyToNonEmptyNodes);
			_dataClient.updateActiveRecord(updRequestSet, null, faultHandler);
		}
		
		protected static function prepareUpdateSymbolRequests(updateRequestSet:RecordUpdateRequestSetProxy, node:NodeProxy, symbol:FieldSymbol, fieldIdx:Number, applyToNonEmptyNodes:Boolean = false):void {
			if( node is EntityProxy ){
				var entity:EntityProxy = node as EntityProxy;
				var children:IList = entity.getChildren();
				for each (var child:NodeProxy in children) {
					prepareUpdateSymbolRequests(updateRequestSet, child, symbol, fieldIdx);
				}
			} else {
				var attr:AttributeProxy = AttributeProxy(node);
				var r:RecordUpdateRequestProxy;
				var field:FieldProxy;
				if(isNaN(fieldIdx) || fieldIdx < 0){
					for(var index:int = 0; index < attr.fields.length; index ++) {
						if ( ! skippedField(attr, index) ) {
							field = attr.fields[index];
							if ( applyToNonEmptyNodes || (field.value == null && field.symbol == null)) {
								r = createUpdateSymbolOperation(node, field, index, symbol);
								updateRequestSet.addRequest(r);
							}
						}
					}
				} else {
					field = attr.fields[fieldIdx];
					if ( applyToNonEmptyNodes || (field.value == null && field.symbol == null)) {
						r = createUpdateSymbolOperation(node, field, fieldIdx, symbol);
						updateRequestSet.addRequest(r);
					}
				}
			}
		}
		
		private static function createUpdateSymbolOperation(node:NodeProxy, field:FieldProxy, fieldIdx:int, symbol:FieldSymbol):RecordUpdateRequestProxy {
			var r:FieldUpdateRequestProxy = new FieldUpdateRequestProxy();
			r.nodeId = node.id;
			r.fieldIndex = fieldIdx;
			r.remarks = field.remarks;
			r.symbol = symbol;
			if ( FieldProxy.isReasonBlankSymbol(symbol) ) {
				r.value = null;
			} else {
				r.value = field.value != null ? field.value.toString(): null;
			}
			return r;
		}
		
		private static function skippedField(attr:AttributeProxy, index:int):Boolean {
			if ( attr.definition is NumberAttributeDefinitionProxy && index == 1 || 
				attr.definition is RangeAttributeDefinitionProxy && index == 2 ) {
				//OFC-720
				return true;
			} else {
				return false;
			}
		}

		protected static function prepareApproveMissingRequests(updateRequestSet:RecordUpdateRequestSetProxy, node:NodeProxy, fieldIdx:Number, applyToNonEmptyNodes:Boolean = true):void {
			if( node is EntityProxy ){
				var entity:EntityProxy = node as EntityProxy;
				var children:IList = entity.getChildren();
				for each (var child:NodeProxy in children) {
					prepareApproveMissingRequests(updateRequestSet, child, fieldIdx, applyToNonEmptyNodes);
				}
			} else {
				var attr:AttributeProxy = AttributeProxy(node);
				var r:RecordUpdateRequestProxy;
				var field:FieldProxy;
				if(isNaN(fieldIdx) || fieldIdx < 0){
					for(var index:int = 0; index < attr.fields.length; index ++) {
						field = attr.fields[index];
						if(applyToNonEmptyNodes || (field.value == null && (field.symbol == null || field.hasReasonBlankSpecified()))) {
							r = createApproveMissingOperation(node, index);
							updateRequestSet.addRequest(r);
						}
					}
				} else {
					field = attr.fields[fieldIdx];
					if(applyToNonEmptyNodes || (field.value == null && (field.symbol == null || field.hasReasonBlankSpecified()))) {
						r = createApproveMissingOperation(node, fieldIdx);
						updateRequestSet.addRequest(r);
					}
				}
			}
		}
		
		private static function createApproveMissingOperation(node:NodeProxy, fieldIdx:int):RecordUpdateRequestProxy {
			var r:MissingValueApproveRequestProxy = new MissingValueApproveRequestProxy();
			r.parentEntityId = node.parentId;
			r.nodeName = node.name;
			return r;
		}
		
		protected static function confirmErrorHandler(event:NodeEvent):void {
			var updRequest:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy();
			var op:RecordUpdateRequestProxy;
			if ( event.node != null ) {
				op = createConfirmErrorOperation(event.node);
				updRequest.addRequest(op);
			} else {
				for each (var node:NodeProxy in event.nodes) {
					var a:AttributeProxy = AttributeProxy(node);
					if ( a.hasErrors() && ! a.errorConfirmed ) { 
						op = createConfirmErrorOperation(node);
						updRequest.addRequest(op);
					}
				}
			}
			_dataClient.updateActiveRecord(updRequest, null, faultHandler);
		}
		
		protected static function createConfirmErrorOperation(node:NodeProxy):RecordUpdateRequestProxy {
			var updRequestOp:ConfirmErrorRequestProxy = new ConfirmErrorRequestProxy();
			updRequestOp.nodeId = node.id;
			return updRequestOp;
		}
		
		protected static function deleteNodeHandler(event:NodeEvent):void {
			var node:NodeProxy = event.node;
			var updRequestOp:NodeDeleteRequestProxy = new NodeDeleteRequestProxy();
			updRequestOp.nodeId = node.id;
			var updRequest:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy(updRequestOp);
			_dataClient.updateActiveRecord(updRequest, null, faultHandler);
		}
		
		protected function keyFocusChangeHandler(event:FocusEvent):void {
			//handled by tabKeyHandler
			event.preventDefault();
			event.stopImmediatePropagation();
		}
		
		protected function updateResponseReceivedHandler(event:ApplicationEvent):void {
			if(_view.attribute != null) {
				var responseSet:RecordUpdateResponseSetProxy = RecordUpdateResponseSetProxy(event.result);
				for each (var response:NodeUpdateResponseProxy in responseSet.responses) {
					if ( response is AttributeUpdateResponseProxy && 
							AttributeUpdateResponseProxy(response).nodeId == _view.attribute.id) {
						_view.changed = false
						updateView();
						return;
					}
				}
			}
		}
		
		protected function setAttribute(value:AttributeProxy):void {
			_view.changed = false;
			_view.visited = false;
			_view.updating = false;
			updateView();
		}
		
		protected function changeHandler(event:Event):void {
			//TODO if autocomplete enabled show autocomplete popup...
			_view.changed = true;
			var inputFieldEvent:InputFieldEvent = new InputFieldEvent(InputFieldEvent.CHANGING);
			_view.dispatchEvent(inputFieldEvent);
		}
		
		protected function focusInHandler(event:FocusEvent):void {
			UIUtil.ensureElementIsVisible(event.target);
		}
		
		protected function focusOutHandler(event:FocusEvent):void {
			if(_view.applyChangesOnFocusOut && _view.changed) {
				updateValue();
			}
			_view.visited = true;
			var inputFieldEvent:InputFieldEvent = new InputFieldEvent(InputFieldEvent.VISITED);
			inputFieldEvent.inputField = _view;
			eventDispatcher.dispatchEvent(inputFieldEvent);
		}
		
		protected function keyDownHandler(event:KeyboardEvent):void {
			var attrDefn:AttributeDefinitionProxy = _view.attributeUIModelObject.attributeDefinition;
			var directionByColumns:Boolean = _view.attributeUIModelObject is ColumnProxy && 
				ColumnProxy(_view.attributeUIModelObject).parentTable.direction == UIOptions$Direction.BY_COLUMNS;
			var keyCode:uint = event.keyCode;
			var offset:int = 0;
			var moveByEntity:Boolean = ! directionByColumns;
			switch(keyCode) {
				case Keyboard.ESCAPE:
					undoLastChange();
					break;
				case Keyboard.TAB:
					handleTabKey(event.shiftKey);
					break;
				case Keyboard.DOWN:
					offset = 1;
					break;
				case Keyboard.UP:
					offset = -1;
					break;
				case Keyboard.PAGE_DOWN:
					offset = 10;
					break;
				case Keyboard.PAGE_UP:
					offset = - 10;
					break;
			}
			if ( offset != 0 ) {
				if ( moveByEntity ) {
					setFocusOnSiblingEntity(offset);
				} else {
					setFocusOnSiblingAttribute(offset);
				}
			}
		}
		
		protected function handleTabKey(shiftKey:Boolean = false):void {
			var attrDefn:AttributeDefinitionProxy = _view.attributeUIModelObject.attributeDefinition;
			var directionByColumns:Boolean = _view.attributeUIModelObject is ColumnProxy && 
				ColumnProxy(_view.attributeUIModelObject).parentTable.direction == UIOptions$Direction.BY_COLUMNS;
			var focusChanged:Boolean = false;
			if ( directionByColumns ) {
				var offset:int = shiftKey ? -1: 1;
				var siblingFocusableField:FieldProxy = getSiblingFocusableFieldInAttribute(getField(), ! shiftKey);
				if ( siblingFocusableField == null ) {
					focusChanged = setFocusOnSiblingEntity(offset, true, false);
				}
			}
			if ( !focusChanged ) {
				UIUtil.moveFocus(shiftKey);
			}
		}
		
		protected function setFocusOnSiblingAttribute(offset:int):Boolean {
			var currentField:FieldProxy = _view.attribute.getField(_view.fieldIndex);
			var fieldToFocusIn:FieldProxy = getSiblingFocusableField(currentField, offset);
			if ( fieldToFocusIn != null ) {
				dispatchFocusSetEvent(fieldToFocusIn);
				return true;
			} else {
				return false;
			}
		}
		
		protected function getSiblingFocusableField(field:FieldProxy, offset:int, limit:Boolean = true):FieldProxy {
			var parentMultipleEntity:EntityProxy = field.parent.getParentMultipleEntity();
			if ( parentMultipleEntity != null ) {
				var siblingFields:IList = getLeafFocusableFields(parentMultipleEntity);
				var currentFieldIndex:int = siblingFields.getItemIndex(field);
				var siblingFieldIndex:int = currentFieldIndex + offset;
				if ( siblingFieldIndex < 0 ) {
					if ( limit ) {
						siblingFieldIndex = 0;
					} else {
						return null;
					}
				} else if ( siblingFieldIndex > siblingFields.length - 1 ) {
					if ( limit ) {
						siblingFieldIndex = siblingFields.length - 1;
					} else {
						return null;
					}
				}
				if ( siblingFieldIndex != currentFieldIndex ) {
					return siblingFields.getItemAt(siblingFieldIndex) as FieldProxy;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		
		protected function dispatchFocusSetEvent(field:FieldProxy):void {
			var attributeToFocusIn:AttributeProxy = field.parent;
			var inputFieldEvent:InputFieldEvent = new InputFieldEvent(InputFieldEvent.SET_FOCUS);
			inputFieldEvent.fieldIdx = attributeToFocusIn is CodeAttributeProxy ? -1: field.index;
			inputFieldEvent.attributeId = attributeToFocusIn.id;
			inputFieldEvent.nodeName = attributeToFocusIn.name;
			inputFieldEvent.parentEntityId = attributeToFocusIn.parentId;
			eventDispatcher.dispatchEvent(inputFieldEvent);
		}
		
		public static function getLeafFocusableFields(entity:EntityProxy):IList {
			var result:ArrayCollection = new ArrayCollection();
			var leafFields:IList = entity.getLeafFields();
			for each (var f:FieldProxy in leafFields) {
				if ( isFieldFocusable(f) ) {
					result.addItem(f);
				}
			}
			return result;
		}
		
		public static function getLeafFocusableAttributes(entity:EntityProxy):IList {
			var result:ArrayCollection = new ArrayCollection();
			var leafAttributes:IList = entity.getLeafAttributes();
			for each (var a:AttributeProxy in leafAttributes) {
				result.addItem(a);
			}
			return result;
		}
		
		public static function isFieldFocusable(field:FieldProxy):Boolean {
			if ( ! (field.parent is CodeAttributeProxy) || 
				! (field.parent.definition as CodeAttributeDefinitionProxy).enumeratingAttribute && field.index == 0 ) {
				return true;
			} else {
				return false;
			}
		}
		
		public static function getSiblingFocusableFieldInAttribute(field:FieldProxy, forward:Boolean = true):FieldProxy {
			var attr:AttributeProxy = field.parent;
			var siblingFieldIndex:int;
			if ( attr.definition is DateAttributeDefinitionProxy ) {
				switch ( field.index ) {
					case 0: //year
						siblingFieldIndex = forward ? -1: 1;
						break;
					case 1: //month
						siblingFieldIndex = forward ? 0: 2;
						break;
					case 2: //date
						siblingFieldIndex = forward ? 1: -1;
						break;
				}
			} else if ( attr.definition is CodeAttributeDefinitionProxy ) {
				//a single field is focusable
				siblingFieldIndex = -1;
			} else {
				siblingFieldIndex = field.index + forward ? 1: -1;
			}
			if ( siblingFieldIndex >= 0 && siblingFieldIndex < attr.fields.length ) {
				return attr.getField(siblingFieldIndex);
			} else {
				return null;
			}
		}
		
		public static function getFirstFocusableFieldIndex(attrDefn:AttributeDefinitionProxy):int {
			if ( attrDefn is DateAttributeDefinitionProxy ) {
				return 2;
			} else {
				return 0;
			}
		}
		
		public static function getLastFocusableFieldIndex(attrDefn:AttributeDefinitionProxy):int {
			if ( attrDefn is DateAttributeDefinitionProxy ) {
				return 0;
			} else {
				return 0;
			}
		}
		
		protected function setFocusOnSiblingEntity(offset:int, circularLookup:Boolean = false, sameFieldIndex:Boolean = true):Boolean {
			var attributeToFocusIn:AttributeProxy;
			var attrDefn:AttributeDefinitionProxy = _view.attributeUIModelObject.attributeDefinition;
			if ( attrDefn.multiple ) {
				var attribute:AttributeProxy = _view.attribute;
				attributeToFocusIn = AttributeProxy(attribute.getSibling(offset));
			} else {
				var parentMultipleEntity:EntityProxy = _view.attribute.getParentMultipleEntity();
				var siblingEntity:EntityProxy = EntityProxy(parentMultipleEntity.getSibling(offset, circularLookup));
				if ( siblingEntity != null ) {
					attributeToFocusIn = siblingEntity.getDescendantSingleAttribute(attrDefn.id);
					var circularLookupApplied:Boolean = circularLookup && siblingEntity.index - parentMultipleEntity.index != offset;
					if ( circularLookupApplied ) {
						var siblingField:FieldProxy = getSiblingFocusableField(attributeToFocusIn.getField(0), offset > 0 ? 1: -1, false);
						if ( siblingField == null || siblingField.parent == attributeToFocusIn ) {
							attributeToFocusIn = null;
						} else {
							attributeToFocusIn = siblingField.parent;
						}
					}
				}
			}
			if ( attributeToFocusIn != null ) {
				var fieldIndex:int;
				if ( sameFieldIndex ) {
					fieldIndex = _view.fieldIndex;
				} else if ( offset > 0 ) {
					fieldIndex = getFirstFocusableFieldIndex(attrDefn);
				} else {
					fieldIndex = getLastFocusableFieldIndex(attrDefn);
				}
				var fieldToFocusIn:FieldProxy = attributeToFocusIn.getField(fieldIndex);
				dispatchFocusSetEvent(fieldToFocusIn);
				return true;
			} else {
				return false;
			}
		}
		
		public function undoLastChange():void {
			_view.changed = false;
			updateView();
		}
		
		public function updateValue():void {
			var r:RecordUpdateRequestProxy = createValueUpdateRequest();
			sendUpdateRequest(r);
		}
		
		public function createAttributeAddRequest(value:String = null, symbol:FieldSymbol = null, remarks:String = null):AttributeAddRequestProxy {
			var r:AttributeAddRequestProxy = new AttributeAddRequestProxy();
			r.parentEntityId = _view.parentEntity.id;
			r.nodeName = _view.attributeUIModelObject.attributeDefinition.name;
			r.value = value;
			r.symbol = symbol;
			r.remarks = remarks;
			return r;
		}
		
		public function createValueUpdateRequest():RecordUpdateRequestProxy {
			var symbol:FieldSymbol = null;
			var value:String = null;
			var text:String = textToRequestValue();
			if ( FieldProxy.isShortCutForReasonBlank(text) ) {
				symbol = FieldProxy.parseShortCutForReasonBlank(text);
			} else {
				value = text;
			}
			var remarks:String = getRemarks(); //preserve old remarks
			var r:RecordUpdateRequestProxy = createSpecificValueUpdateRequest(value, symbol, remarks);
			return r;
		}

		protected function sendUpdateRequest(o:RecordUpdateRequestProxy):void {
			var req:RecordUpdateRequestSetProxy = new RecordUpdateRequestSetProxy(o);
			dataClient.updateActiveRecord(req, updateResultHandler, faultHandler);
			_view.updating = true;
		}
		
		protected function createSpecificValueUpdateRequest(value:String, symbol:FieldSymbol = null, remarks:String = null):RecordUpdateRequestProxy {
			if ( _view.fieldIndex >= 0 ) {
				var fieldUpdReq:FieldUpdateRequestProxy = new FieldUpdateRequestProxy();
				fieldUpdReq.nodeId = _view.attribute.id;
				fieldUpdReq.fieldIndex = _view.fieldIndex;
				fieldUpdReq.value = value;
				fieldUpdReq.symbol = symbol;
				fieldUpdReq.remarks = remarks;
				return fieldUpdReq;
			} else {
				var attrUpdReq:AttributeUpdateRequestProxy = new AttributeUpdateRequestProxy();
				attrUpdReq.nodeId = _view.attribute.id;
				attrUpdReq.value = value;
				attrUpdReq.symbol = symbol;
				attrUpdReq.remarks = remarks;
				return attrUpdReq;	
			}
		}
		
		protected function updateResultHandler(event:ResultEvent, token:Object = null):void {
			_view.changed = false;
			_view.updating = false;
			//_view.currentState = InputField.STATE_SAVE_COMPLETE;
		}
		
		protected function getTextFromValue():String {
			var attribute:AttributeProxy = _view.attribute;
			if(attribute != null) {
				var field:FieldProxy = _view.attribute.getField(_view.fieldIndex);
				return field.getValueAsText();
			} else {
				return "";
			}
		}

		protected function textToRequestValue():String {
			var result:String = null;
			var text:String = _view.text;
			if(StringUtil.isNotBlank(text)) {
				result = StringUtil.trim(text);
			}
			return result;
		}
		
		protected function updateView():void {
			//update view according to attribute (generic text value)
			var hasRemarks:Boolean = false;
			if(_view.attributeUIModelObject.attributeDefinition != null) {
				var text:String = getTextFromValue();
				if ( ! _view.changed ) {
					_view.text = text;
				}
				hasRemarks = StringUtil.isNotBlank(getRemarks());
				_contextMenu.updateItems();
			}
			
			var newStyles:Array = [];
			if ( hasRemarks ) {
				newStyles.push(InputField.REMARKS_PRESENT_STYLE);
			}
			if ( ! Application.activeRecordEditable ) {
				newStyles.push(InputField.READONLY_STYLE);	
			}
			UIUtil.replaceStyleNames(_view.validationStateDisplay, newStyles, 
				[InputField.REMARKS_PRESENT_STYLE, InputField.READONLY_STYLE] );
			
			//_view.hasRemarks = hasRemarks;
			_view.editable = Application.activeRecordEditable;
		}
		
		protected function getField():FieldProxy {
			if(_view.attribute != null) {
				var fieldIndex:int = 0;
				if(_view.fieldIndex >= 0) {
					fieldIndex = _view.fieldIndex;
				}
				return _view.attribute.getField(fieldIndex);
			}
			return null;
		}
		
		protected function getRemarks():String {
			var f:FieldProxy = getField();
			if(f != null) {
				return f.remarks;
			} 
			return null;
		}
		
		protected function getSymbol():FieldSymbol {
			var f:FieldProxy = getField();
			if(f != null) {
				return f.symbol;
			} 
			return null;
		}
		
		protected function get contextMenu():InputFieldContextMenu {
			return _contextMenu;
		}

		protected static function get dataClient():DataClient {
			return _dataClient;
		}
	}
}
