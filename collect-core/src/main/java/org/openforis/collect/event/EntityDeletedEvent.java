package org.openforis.collect.event;

import java.util.Date;
import java.util.List;

public class EntityDeletedEvent extends RecordEvent {

	public EntityDeletedEvent(String surveyName, Integer recordId,
			String definitionId, List<String> ancestorIds, String nodeId,
			Date timestamp, String userName) {
		super(surveyName, recordId, definitionId, ancestorIds, nodeId, timestamp,
				userName);
	}

}