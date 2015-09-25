package org.openforis.collect.datacleansing.controller;

import org.openforis.collect.datacleansing.DataErrorType;
import org.openforis.collect.datacleansing.form.DataErrorTypeForm;
import org.openforis.collect.datacleansing.form.validation.DataErrorTypeValidator;
import org.openforis.collect.datacleansing.manager.DataErrorTypeManager;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.web.controller.AbstractSurveyObjectEditFormController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;

@Controller
@Scope(value=WebApplicationContext.SCOPE_SESSION)
@RequestMapping(value = "/datacleansing/dataerrortypes")
public class DataErrorTypeController extends AbstractSurveyObjectEditFormController<DataErrorType, DataErrorTypeForm, DataErrorTypeManager> {
	
	@Autowired
	private DataErrorTypeValidator validator;
	
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}
	
	@Override
	@Autowired
	@Qualifier("dataErrorTypeManager")
	public void setItemManager(DataErrorTypeManager itemManager) {
		super.setItemManager(itemManager);
	}
	
	@Override
	protected DataErrorTypeForm createFormInstance(DataErrorType item) {
		return new DataErrorTypeForm(item);
	}
	
	protected DataErrorType createItemInstance(CollectSurvey survey) {
		return new DataErrorType(survey);
	};
}