/**
 * Generated by Gas3 v2.2.0 (Granite Data Services).
 *
 * NOTE: this file is only generated if it does not exist. You may safely put
 * your custom code here.
 */

package org.openforis.collect.metamodel.proxy {
	import mx.collections.IList;

    [Bindable]
    [RemoteClass(alias="org.openforis.collect.metamodel.proxy.LanguageSpecificTextProxy")]
    public class LanguageSpecificTextProxy extends LanguageSpecificTextProxyBase {
		
		public static function getLocalizedText(list:IList, language:String = null, firstIfNotFound:Boolean = true):String {
			for each (var item:LanguageSpecificTextProxy in list) {
				if(item.language == language) {
					return item.text;
				}
			}
			if(firstIfNotFound && list != null && list.length > 0) {
				return LanguageSpecificTextProxy(list.getItemAt(0)).text;
			} else {
				return null;
			}
		}
		
    }
}