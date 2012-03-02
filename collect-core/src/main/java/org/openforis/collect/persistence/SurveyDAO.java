package org.openforis.collect.persistence;

import static org.openforis.collect.persistence.jooq.Sequences.OFC_SCHEMA_DEFINITION_ID_SEQ;
import static org.openforis.collect.persistence.jooq.Sequences.OFC_SURVEY_ID_SEQ;
import static org.openforis.collect.persistence.jooq.tables.OfcSchemaDefinition.OFC_SCHEMA_DEFINITION;
import static org.openforis.collect.persistence.jooq.tables.OfcSurvey.OFC_SURVEY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.Factory;
import org.jooq.impl.SQLDataType;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.persistence.jooq.JooqDaoSupport;
import org.openforis.collect.persistence.xml.CollectIdmlBindingContext;
import org.openforis.idm.metamodel.NodeDefinition;
import org.openforis.idm.metamodel.Schema;
import org.openforis.idm.metamodel.Survey;
import org.openforis.idm.metamodel.xml.InvalidIdmlException;
import org.openforis.idm.metamodel.xml.SurveyMarshaller;
import org.openforis.idm.metamodel.xml.SurveyUnmarshaller;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author G. Miceli
 * @author M. Togna
 */
public class SurveyDAO extends JooqDaoSupport {
//	private final Log LOG = LogFactory.getLog(SurveyDAO.class);
	
	private CollectIdmlBindingContext bindingContext;

	public SurveyDAO() {
		bindingContext = new CollectIdmlBindingContext();
	}
	@Transactional
	public void importModel(Survey survey) throws SurveyImportException {
		String name = survey.getName();
		if ( StringUtils.isBlank(name) ) {
			throw new SurveyImportException("Survey name must be set before importing");
		}
		
		String idml = marshalSurvey(survey);

		// Insert into OFC_SURVEY table
		Factory jf = getJooqFactory();
		int surveyId = jf.nextval(OFC_SURVEY_ID_SEQ).intValue();
		jf.insertInto(OFC_SURVEY)
			.set(OFC_SURVEY.ID, surveyId)
			.set(OFC_SURVEY.NAME, name)
			.set(OFC_SURVEY.IDML, Factory.val(idml, SQLDataType.CLOB))
			.execute();

		survey.setId(surveyId);

		// Insert SCHEMA_DEFINITIONs
		Schema schema = survey.getSchema();
		Collection<NodeDefinition> definitions = schema.getAllDefinitions();
		for (NodeDefinition definition : definitions) {
			int definitionId = jf.nextval(OFC_SCHEMA_DEFINITION_ID_SEQ).intValue();
			String path = definition.getPath();
			jf.insertInto(OFC_SCHEMA_DEFINITION)
				.set(OFC_SCHEMA_DEFINITION.ID, definitionId)
				.set(OFC_SCHEMA_DEFINITION.SURVEY_ID, surveyId)
				.set(OFC_SCHEMA_DEFINITION.PATH, path)
				.execute();
			definition.setId(definitionId);
		}
	}
	
	public Survey load(int id) {
		Factory jf = getJooqFactory();
		Record record = jf.select()
				.from(OFC_SURVEY)
				.where(OFC_SURVEY.ID.equal(id))
				.fetchOne();
		Survey survey = processSurveyRow(record);
		if ( survey != null ) {
			loadNodeDefinitions(survey);
		}
		return survey;
	}

	public CollectSurvey load(String name) {
		Factory jf = getJooqFactory();
		Record record = jf.select()
				.from(OFC_SURVEY)
				.where(OFC_SURVEY.NAME.equal(name))
				.fetchOne();
		CollectSurvey survey = processSurveyRow(record);
		if ( survey != null ) {
			loadNodeDefinitions(survey);
		}
		return survey;
	}

	@Transactional
	public List<CollectSurvey> loadAll() {
		Factory jf = getJooqFactory();
		List<CollectSurvey> surveys = new ArrayList<CollectSurvey>();
		Result<Record> results = jf.select().from(OFC_SURVEY).fetch();
		for (Record row : results) {
			CollectSurvey survey = processSurveyRow(row);
			if (survey != null) {
				loadNodeDefinitions(survey);
				surveys.add(survey);
			}
		}
		return surveys;
	}

	private CollectSurvey processSurveyRow(Record row) {
		try {
			if (row == null) {
				return null;
			}
			String idml = row.getValueAsString(OFC_SURVEY.IDML);
			CollectSurvey survey = (CollectSurvey) unmarshalIdml(idml);
			survey.setId(row.getValueAsInteger(OFC_SURVEY.ID));
			return survey;
		} catch (IOException e) {
			throw new RuntimeException("Error deserializing IDML from database", e);
		}
	}

	private Survey unmarshalIdml(String idml) throws IOException {
		byte[] bytes = idml.getBytes("UTF-8");
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		SurveyUnmarshaller su = bindingContext.createSurveyUnmarshaller();
		CollectSurvey survey;
		try {
			survey = (CollectSurvey) su.unmarshal(is);
		} catch (InvalidIdmlException e) {
			throw new DataInconsistencyException("Invalid idm");
		}
		return survey;
	}

	private void loadNodeDefinitions(Survey survey) {
		Factory jf = getJooqFactory();
		// Internal IDs by path and associate with each node in tree
		Schema schema = survey.getSchema();
		Result<Record> result = jf.select().from(OFC_SCHEMA_DEFINITION).where(OFC_SCHEMA_DEFINITION.SURVEY_ID.equal(survey.getId())).fetch();
		for (Record defnRecord : result) {
			int defnId = defnRecord.getValueAsInteger(OFC_SCHEMA_DEFINITION.ID);
			String path = defnRecord.getValueAsString(OFC_SCHEMA_DEFINITION.PATH);
			NodeDefinition defn = schema.getByPath(path);
			defn.setId(defnId);
		}
	}

	private String marshalSurvey(Survey survey) throws SurveyImportException {
		try {
			// Serialize Survey to XML
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			SurveyMarshaller sm = bindingContext.createSurveyMarshaller();			
			sm.setIndent(true);
			sm.marshal(survey, os);
			return os.toString("UTF-8");
		} catch (IOException e) {
			throw new SurveyImportException("Error unmarshalling survey", e);
		} 
	}

}
