/**
 * 
 */
package org.openforis.collect.metamodel.ui;

import java.util.Collections;
import java.util.List;

import org.openforis.idm.metamodel.LanguageSpecificText;
import org.openforis.idm.metamodel.LanguageSpecificTextMap;

/**
 * @author S. Ricci
 *
 */
public class Form extends FormContentContainer {

	private static final long serialVersionUID = 1L;

	private LanguageSpecificTextMap labels;
	
	public <P extends FormContentContainer> Form(P parent, int id) {
		super(parent, id);
	}

	public List<LanguageSpecificText> getLabels() {
		if ( labels == null ) {
			return Collections.emptyList();
		} else {
			return labels.values();
		}
	}
	
	public String getLabel(String language) {
		return labels == null ? null: labels.getText(language);
	}
	
	public void addLabel(LanguageSpecificText label) {
		if ( labels == null ) {
			labels = new LanguageSpecificTextMap();
		}
		labels.add(label);
	}

	public void setLabel(String language, String text) {
		if ( labels == null ) {
			labels = new LanguageSpecificTextMap();
		}
		labels.setText(language, text);
	}
	
	public void removeLabel(String language) {
		labels.remove(language);
	}

}
