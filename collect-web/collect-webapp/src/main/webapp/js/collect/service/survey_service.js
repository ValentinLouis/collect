Collect.SurveyService = function() {
	Collect.AbstractService.apply(this, arguments);
	this.contextPath = "surveys/";
};

Collect.SurveyService.prototype = Object.create(Collect.AbstractService.prototype);

Collect.SurveyService.prototype.loadSummaries = function(onSuccess, onError, excludeTemporary) {
	this.send("summaries.json", {includeTemporary: ! excludeTemporary}, "GET",  onSuccess, onError);
};

Collect.SurveyService.prototype.loadFullPublishedSurveys = function(onSuccess, onError) {
	this.send("published/full-list.json", null, "GET",  onSuccess, onError);
};