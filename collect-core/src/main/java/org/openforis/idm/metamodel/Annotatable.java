package org.openforis.idm.metamodel;

import java.util.Set;

import javax.xml.namespace.QName;

/**
 * @author G. Miceli
 * @author M. Togna
 */
public interface Annotatable {
	
	String getAnnotation(QName qname);
	
	void setAnnotation(QName qname, String value);

	Set<QName> getAnnotationNames();
	
}
