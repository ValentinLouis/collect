/**
 * 
 */
package org.openforis.collect.io.data;

import org.openforis.collect.manager.RecordManager;
import org.openforis.collect.manager.UserManager;
import org.openforis.concurrency.Worker;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author S. Ricci
 *
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DataRestoreSummaryJob extends DataRestoreBaseJob {

	//input
	private boolean fullSummary;
	//output
	private DataImportSummary summary;

	@Override
	protected void buildTasks() throws Throwable {
		super.buildTasks();
		addTask(DataRestoreSummaryTask.class);
	}
	
	@Override
	protected boolean isRecordProviderToBeInitialized() {
		return isFullSummary() || oldBackupFormat || dataSummaryFile == null;
	}
	
	@Override
	protected void initializeTask(Worker task) {
		if ( task instanceof DataRestoreSummaryTask ) {
			DataRestoreSummaryTask t = (DataRestoreSummaryTask) task;
			t.setRecordManager(recordManager);
			t.setRecordFileManager(recordFileManager);
			t.setUserManager(userManager);
			t.setRecordProvider(recordProvider);
			t.setFullSummary(fullSummary);
			t.setDataSummaryFile(dataSummaryFile);
			t.setOldFormat(oldBackupFormat);
			t.setSurvey(publishedSurvey);
			t.setIncludeRecordPredicate(includeRecordPredicate);
		}
		super.initializeTask(task);
	}
	
	@Override
	protected void onTaskCompleted(Worker task) {
		super.onTaskCompleted(task);
		if ( task instanceof DataRestoreSummaryTask ) {
			//get output survey and set it into job instance instance variable
			this.summary = ((DataRestoreSummaryTask) task).getSummary();
		}
	}

	public UserManager getUserManager() {
		return userManager;
	}
	
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}
	
	public RecordManager getRecordManager() {
		return recordManager;
	}
	
	public void setRecordManager(RecordManager recordManager) {
		this.recordManager = recordManager;
	}
	
	public DataImportSummary getSummary() {
		return summary;
	}
	
	public boolean isFullSummary() {
		return fullSummary;
	}
	
	public void setFullSummary(boolean fullSummary) {
		this.fullSummary = fullSummary;
		super.setValidateRecords(fullSummary);
	}
	
	@Override
	public void setValidateRecords(boolean validateRecords) {
		throw new UnsupportedOperationException();
	}

}
