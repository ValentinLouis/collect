package org.openforis.collect.event;

import java.util.Date;
import java.util.List;

public abstract class AttributeUpdatedEvent extends AttributeEvent {

	public AttributeUpdatedEvent(String surveyName, Integer recordId,
			RecordStep step, String definitionId, List<String> ancestorIds,
			String nodeId, Date timestamp, String userName) {
		super(surveyName, recordId, step, definitionId, ancestorIds, nodeId,
				timestamp, userName);
	}

}
