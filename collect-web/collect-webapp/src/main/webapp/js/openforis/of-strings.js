OF.Strings = function() {};

OF.Strings.isBlank = function(value) {
	return value == null || value.trim().length == 0;
};

OF.Strings.isNotBlank = function(value) {
	return ! OF.Strings.isBlank(value);
};

OF.Strings.firstNotBlank = function() {
	var values = arguments;
	for(var i=0; i < values.length; i++) {
		var value = values[i];
		if (OF.Strings.isNotBlank(value)) {
			return value;
		}
	}
	return null;
};

OF.Strings.escapeHtml = function(str) {
	return $('<div>').text(str).html();
};