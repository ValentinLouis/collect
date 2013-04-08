/**
 * 
 */
package org.openforis.collect.persistence.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.openforis.collect.metamodel.ui.FormBundle;
import org.openforis.collect.metamodel.ui.UIOptions;
import org.openforis.collect.metamodel.ui.UIOptionsConstants;
import org.openforis.collect.metamodel.ui.UITabSet;
import org.openforis.collect.persistence.DataInconsistencyException;
import org.openforis.collect.persistence.xml.internal.marshal.UIOptionsSerializer;
import org.openforis.collect.persistence.xml.internal.unmarshal.FormBundlesPR;
import org.openforis.collect.persistence.xml.internal.unmarshal.UITabSetPR;
import org.openforis.idm.metamodel.xml.ApplicationOptionsBinder;
import org.openforis.idm.metamodel.xml.XmlParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * @author S. Ricci
 *
 */
public class UIOptionsBinder implements
		ApplicationOptionsBinder<UIOptions> {

	protected UIOptions uiOptions;
	
	@Override
	public UIOptions unmarshal(String type, String body) {
		XmlPullParser parser = null;
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			parser = factory.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
			
			unmarshal(body, parser);
			
			if ( uiOptions == null || uiOptions.getFormBundles().isEmpty() ) {
				//backwards compatibility
				unmarshalTabSets(body, parser);
				//TODO convert to new ui model...
			}
			return uiOptions;
		} catch (Exception e) {
			throw new DataInconsistencyException(e.getMessage(), e);
		}
	}

	protected void unmarshal(String body, XmlPullParser parser)
			throws DataInconsistencyException {
		try {
			uiOptions = new UIOptions();
			Reader reader = new StringReader(body);
			parser.setInput(reader);
			unmarshalFormBundle(parser, uiOptions);
		} catch ( Exception e ) {
			if ( UIOptionsConstants.TAB_SET.equals(parser.getName()) && 
					UIOptionsConstants.UI_NAMESPACE_URI.equals(parser.getNamespace()) ) {
				//old format
			} else {
				throw new DataInconsistencyException(e);
			}
		}
	}

	protected void unmarshalTabSets(String body, XmlPullParser parser)
			throws XmlPullParserException, IOException, XmlParseException {
		uiOptions = new UIOptions();
		Reader reader = new StringReader(body);
		parser.setInput(reader);
		UITabSet tabSet = unmarshalTabSet(parser, uiOptions);
		while ( tabSet != null ) {
			uiOptions.addTabSet(tabSet);
			tabSet = unmarshalTabSet(parser, uiOptions);
		}
	}

	protected UITabSet unmarshalTabSet(XmlPullParser parser, UIOptions uiOptions) throws IOException, XmlPullParserException, XmlParseException {
		try {
			UITabSetPR tabSetPR = new UITabSetPR(this);
			tabSetPR.parse(parser);
			UITabSet tabSet = tabSetPR.getTabSet();
			return tabSet;
		} catch ( XmlParseException e) {
			if ( parser != null && parser.getEventType() == XmlPullParser.END_DOCUMENT ) {
				return null;
			} else {
				throw e;
			}
		}
	}

	protected void unmarshalFormBundle(XmlPullParser parser, UIOptions uiOptions) throws IOException, XmlPullParserException, XmlParseException {
		try {
			FormBundlesPR reader = new FormBundlesPR(this);
			reader.parse(parser);
			List<FormBundle> formBundles = reader.getFormBundles();
			for (FormBundle formBundle : formBundles) {
				uiOptions.addFormBundle(formBundle);
			}
		} catch ( XmlParseException e) {
			if ( parser != null && parser.getEventType() != XmlPullParser.END_DOCUMENT ) {
				throw e;
			}
		}
	}

	@Override
	public String marshal(UIOptions options) {
		try {
			UIOptionsSerializer serializer = new UIOptionsSerializer();
			Writer writer = new StringWriter();
			serializer.write(options, writer);
			String result = writer.toString();
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Error serializing UIOptions", e);
		}
	}

	@Override
	public boolean isSupported(String optionsType) {
		if ( UIOptionsConstants.UI_TYPE.equals(optionsType) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public UIOptions getUiOptions() {
		return uiOptions;
	}

}
