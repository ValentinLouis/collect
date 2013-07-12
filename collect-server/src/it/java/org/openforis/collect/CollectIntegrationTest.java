package org.openforis.collect;

import java.io.IOException;
import java.io.InputStream;

import org.junit.runner.RunWith;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.model.CollectSurveyContext;
import org.openforis.collect.persistence.SurveyDao;
import org.openforis.collect.persistence.SurveyImportException;
import org.openforis.idm.metamodel.xml.IdmlParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author S. Ricci
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:test-context.xml"} )
@TransactionConfiguration(defaultRollback=true)
@Transactional
public abstract class CollectIntegrationTest {

	@Autowired
	protected CollectSurveyContext collectSurveyContext;
	@Autowired
	protected SurveyDao surveyDao;
	
	protected CollectSurvey loadSurvey() throws IdmlParseException, IOException  {
		InputStream is = ClassLoader.getSystemResourceAsStream("test.idm.xml");
		CollectSurvey survey = surveyDao.unmarshalIdml(is);
		survey.setName("archenland1");
		return survey;
	}

	protected CollectSurvey importModel() throws SurveyImportException, IdmlParseException, IOException  {
		CollectSurvey survey = (CollectSurvey) loadSurvey();
		surveyDao.importModel(survey);
		return survey;
	}
	
	protected CollectSurvey createSurvey() {
		CollectSurvey createSurvey = (CollectSurvey) collectSurveyContext.createSurvey();
		return createSurvey;
	}
	
}
