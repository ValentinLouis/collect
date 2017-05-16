package org.openforis.collect.web.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;
import org.openforis.collect.ProxyContext;
import org.openforis.collect.event.EventProducer;
import org.openforis.collect.event.RecordEvent;
import org.openforis.collect.manager.MessageSource;
import org.openforis.collect.manager.RandomRecordGenerator;
import org.openforis.collect.manager.RandomRecordGenerator.Parameters;
import org.openforis.collect.manager.RecordManager;
import org.openforis.collect.manager.SurveyManager;
import org.openforis.collect.manager.UserManager;
import org.openforis.collect.model.CollectRecord;
import org.openforis.collect.model.CollectRecord.Step;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.model.RecordFilter;
import org.openforis.collect.model.RecordSummarySortField;
import org.openforis.collect.model.User;
import org.openforis.collect.model.proxy.RecordProxy;
import org.openforis.collect.persistence.RecordPersistenceException;
import org.openforis.idm.metamodel.EntityDefinition;
import org.openforis.idm.metamodel.Schema;
import org.openforis.idm.metamodel.SurveyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * @author S. Ricci
 * 
 */
@Controller
public class RecordController extends BasicController implements Serializable {

	private static final long serialVersionUID = 1L;

	// private static Log LOG = LogFactory.getLog(DataController.class);

	@Autowired
	private RecordManager recordManager;
	@Autowired
	private SurveyManager surveyManager;
	@Autowired
	private SurveyContext surveyContext;
	@Autowired
	private UserManager userManager;
	@Autowired
	private MessageSource messageSource;
	@Autowired
	private RandomRecordGenerator randomRecordGenerator;
	
	@RequestMapping(value = "survey/{surveyId}/data/records/{recordId}/binary_data.json", method=GET)
	public @ResponseBody
	Map<String, Object> loadData(
			@PathVariable int surveyId,
			@PathVariable int recordId,
			@RequestParam(value="step") Integer stepNumber) throws Exception {
		stepNumber = getStepNumberOrDefault(stepNumber);
		CollectSurvey survey = surveyManager.getById(surveyId);
		byte[] data = recordManager.loadBinaryData(survey, recordId, Step.valueOf(stepNumber));
		byte[] encoded = Base64.encodeBase64(data);
		String result = new String(encoded);
		
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("data", result);
		
 		return map;
	}

	@RequestMapping(value = "survey/{surveyId}/data/records/count.json", method=GET)
	public @ResponseBody
	int getCount(@PathVariable int surveyId,
			@RequestParam(value="rootEntityDefinitionId", required=false) Integer rootEntityDefinitionId,
			@RequestParam(value="step", required=false) Integer stepNumber) throws Exception {
		CollectSurvey survey = surveyManager.getById(surveyId);
		RecordFilter filter = new RecordFilter(survey);
		filter.setRootEntityId(rootEntityDefinitionId);
		if (stepNumber != null) {
			filter.setStepGreaterOrEqual(Step.valueOf(stepNumber));
		}
		int count = recordManager.countRecords(filter);
		return count;
	}
	
	@RequestMapping(value = "survey/{surveyId}/data/records/summary.json", method=GET)
	public @ResponseBody Map<String, Object> loadRecordSummaries(
			@PathVariable int surveyId,
			@Valid RecordSummarySearchParameters params) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		CollectSurvey survey = surveyManager.getById(surveyId);
		Schema schema = survey.getSchema();
		EntityDefinition rootEntityDefinition = params.getRootEntityName() == null ? schema.getFirstRootEntityDefinition() : 
			schema.getRootEntityDefinition(params.getRootEntityName());
		
		RecordFilter filter = new RecordFilter(survey, rootEntityDefinition.getId());
		filter.setKeyValues(params.getKeyValues());
		filter.setOffset(params.getOffset());
		filter.setMaxNumberOfRecords(params.getMaxNumberOfRows());
		
		//load summaries
		List<CollectRecord> summaries = recordManager.loadSummaries(filter, params.getSortFields());
		result.put("records", toProxies(summaries));
		
		//count total records
		int count = recordManager.countRecords(filter);
		result.put("count", count);
		
		return result;
	}

	@RequestMapping(value = "survey/{surveyId}/data/records/{recordId}/edit.htm", method=GET)
	public ModelAndView editRecord(@PathVariable int surveyId, @PathVariable int recordId ) throws Exception {
		URIBuilder uriBuilder = new URIBuilder("redirect:/index.htm");
		uriBuilder.addParameter("edit", "true");
		uriBuilder.addParameter("surveyId", Integer.toString(surveyId));
		uriBuilder.addParameter("recordId", Integer.toString(recordId));
		String url = uriBuilder.toString();
		//String url = String.format("redirect:/index.htm?edit=true&surveyId=%d&recordId=%d", surveyId, recordId);
		return new ModelAndView(url);
	}
	
	@RequestMapping(value = "survey/{surveyId}/data/records/{recordId}/content.json", method=GET, produces=APPLICATION_JSON_VALUE)
	public @ResponseBody
	RecordProxy loadRecord(
			@PathVariable int surveyId, 
			@PathVariable int recordId,
			@RequestParam(value="step", required=false) Integer stepNumber) throws RecordPersistenceException {
		stepNumber = getStepNumberOrDefault(stepNumber);
		CollectSurvey survey = surveyManager.getById(surveyId);
		CollectRecord record = recordManager.load(survey, recordId, Step.valueOf(stepNumber));
		return toProxy(record);
	}

	@Transactional
	@RequestMapping(value = "survey/{surveyId}/data/records/random.json", method=POST, consumes=APPLICATION_JSON_VALUE)
	public @ResponseBody
	List<RecordEvent> createRandomRecord(@PathVariable int surveyId, @RequestBody Parameters params) throws RecordPersistenceException {
		CollectRecord record = randomRecordGenerator.generate(surveyId, params);
		User user = userManager.loadById(params.getUserId());
		List<RecordEvent> events = new EventProducer().produceFor(record, user.getUsername());
		return events;
	}
	
	private Integer getStepNumberOrDefault(Integer stepNumber) {
		if (stepNumber == null) {
			stepNumber = Step.ENTRY.getStepNumber();
		}
		return stepNumber;
	}
	
	private RecordProxy toProxy(CollectRecord record) {
		String defaultLanguage = record.getSurvey().getDefaultLanguage();
		Locale locale = new Locale(defaultLanguage);
		ProxyContext context = new ProxyContext(locale, messageSource, surveyContext);
		return new RecordProxy(record, context);
	}
	
	private List<RecordProxy> toProxies(List<CollectRecord> summaries) {
		List<RecordProxy> result = new ArrayList<RecordProxy>(summaries.size());
		for (CollectRecord summary : summaries) {
			result.add(toProxy(summary));
		}
		return result;
	}

	public static class SearchParameters {
		
		private int offset;
		private int maxNumberOfRows;
		
		public int getOffset() {
			return offset;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public int getMaxNumberOfRows() {
			return maxNumberOfRows;
		}

		public void setMaxNumberOfRows(int maxNumberOfRows) {
			this.maxNumberOfRows = maxNumberOfRows;
		}
	}
	
	public static class RecordSummarySearchParameters extends SearchParameters {
		
		private String rootEntityName;
		private List<RecordSummarySortField> sortFields;
		private String[] keyValues;

		public String getRootEntityName() {
			return rootEntityName;
		}

		public void setRootEntityName(String rootEntityName) {
			this.rootEntityName = rootEntityName;
		}

		public List<RecordSummarySortField> getSortFields() {
			return sortFields;
		}

		public void setSortFields(List<RecordSummarySortField> sortFields) {
			this.sortFields = sortFields;
		}

		public String[] getKeyValues() {
			return keyValues;
		}

		public void setKeyValues(String[] keyValues) {
			this.keyValues = keyValues;
		}
	}
}
