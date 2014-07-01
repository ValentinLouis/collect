package org.openforis.collect.io.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openforis.collect.io.SurveyBackupJob;
import org.openforis.collect.io.data.DataExportStatus.Format;
import org.openforis.collect.manager.RecordFileManager;
import org.openforis.collect.manager.RecordManager;
import org.openforis.collect.manager.SurveyManager;
import org.openforis.collect.manager.exception.RecordFileException;
import org.openforis.collect.manager.process.AbstractProcess;
import org.openforis.collect.model.CollectRecord;
import org.openforis.collect.model.CollectRecord.Step;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.persistence.xml.DataMarshaller;
import org.openforis.idm.metamodel.FileAttributeDefinition;
import org.openforis.idm.model.FileAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 
 * @author S. Ricci
 *
 * @deprecated use {@link SurveyBackupJob} or {@link DataBackupTask} instead.  
 *
 */
@Deprecated
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class XMLDataExportProcess extends AbstractProcess<Void, DataExportStatus> {


	private static Log LOG = LogFactory.getLog(XMLDataExportProcess.class);

	public static final String IDML_FILE_NAME = "idml.xml";
	public static final String ZIP_DIRECTORY_SEPARATOR = "/";
	public static final String RECORD_FILE_DIRECTORY_NAME = "upload";

	@Autowired
	private RecordManager recordManager;
	@Autowired
	private RecordFileManager recordFileManager;
	@Autowired
	private SurveyManager surveyManager;
	@Autowired
	private DataMarshaller dataMarshaller;
	
	private File outputFile;
	private CollectSurvey survey;
	private Step[] steps;
	private String rootEntityName;
	private boolean includeIdm;
	private Date modifiedSince;
	
	public XMLDataExportProcess() {
		super();
		this.steps = Step.values();
		this.includeIdm = true;
	}

	@Override
	protected void initStatus() {
		this.status = new DataExportStatus(Format.XML);
	}
	
	@Override
	public void startProcessing() throws Exception {
		super.startProcessing();
		try {
			List<CollectRecord> recordSummaries = loadAllSummaries();
			if ( recordSummaries != null && steps != null && steps.length > 0 ) {
				if (outputFile.exists()) {
					outputFile.delete();
					outputFile.createNewFile();
				}
				FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
				ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);
				backup(zipOutputStream, recordSummaries);
				zipOutputStream.flush();
				zipOutputStream.close();
			}
		} catch (Exception e) {
			status.error();
			LOG.error("Error during data export", e);
		}
	}

	private void backup(ZipOutputStream zipOutputStream, List<CollectRecord> recordSummaries) {
		int total = calculateTotal(recordSummaries);
		status.setTotal(total);
		if ( includeIdm ) {
			includeIdml(zipOutputStream);
		}
		for (CollectRecord summary : recordSummaries) {
			if ( status.isRunning() ) {
				int recordStepNumber = summary.getStep().getStepNumber();
				for (Step step : steps) {
					int stepNum = step.getStepNumber();
					if ( stepNum <= recordStepNumber ) {
						backup(zipOutputStream, summary, Step.valueOf(stepNum));
						status.incrementProcessed();
					}
				}
			} else {
				break;
			}
		}
	}

	private List<CollectRecord> loadAllSummaries() {
		List<CollectRecord> summaries = recordManager.loadSummaries(survey, rootEntityName, modifiedSince);
		return summaries;
	}
	
	private int calculateTotal(List<CollectRecord> recordSummaries) {
		int count = 0;
		for (CollectRecord summary : recordSummaries) {
			int recordStepNumber = summary.getStep().getStepNumber();
			for (Step step: steps) {
				if ( step.getStepNumber() <= recordStepNumber ) {
					count ++;
				}
			}
		}
		return count;
	}
	
	private void includeIdml(ZipOutputStream zipOutputStream) {
		ZipEntry entry = new ZipEntry(IDML_FILE_NAME);
		try {
			zipOutputStream.putNextEntry(entry);
			surveyManager.marshalSurvey(survey, zipOutputStream, true, true, false);
//			String surveyMarshalled = surveyManager.marshalSurvey(survey);
//			PrintWriter printWriter = new PrintWriter(zipOutputStream);
//			printWriter.write(surveyMarshalled);
			zipOutputStream.closeEntry();
			zipOutputStream.flush();
		} catch (IOException e) {
			String message = "Error while including idml into zip file: " + e.getMessage();
			if (LOG.isErrorEnabled()) {
				LOG.error(message, e);
			}
			throw new RuntimeException(message, e);
		}
	}
	
	private void backup(ZipOutputStream zipOutputStream, CollectRecord summary, Step step) {
		Integer id = summary.getId();
		try {
			CollectRecord record = recordManager.load(survey, id, step);
			RecordEntry recordEntry = new RecordEntry(step, id);
			String entryName = recordEntry.getName();
			ZipEntry entry = new ZipEntry(entryName);
			zipOutputStream.putNextEntry(entry);
			OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream);
			dataMarshaller.write(record, writer);
			zipOutputStream.closeEntry();
			zipOutputStream.flush();
			backupRecordFiles(zipOutputStream, record);
		} catch (Exception e) {
			String message = "Error while backing up " + id + " " + e.getMessage();
			if (LOG.isErrorEnabled()) {
				LOG.error(message, e);
			}
			throw new RuntimeException(message, e);
		}
	}
	
	private void backupRecordFiles(ZipOutputStream zipOutputStream,
			CollectRecord record) throws RecordFileException {
		List<FileAttribute> fileAttributes = record.getFileAttributes();
		for (FileAttribute fileAttribute : fileAttributes) {
			if ( ! fileAttribute.isEmpty() ) {
				File file = recordFileManager.getRepositoryFile(fileAttribute);
				if ( file == null ) {
					String message = String.format("Missing file: %s attributeId: %d attributeName: %s", 
							fileAttribute.getFilename(), fileAttribute.getInternalId(), fileAttribute.getName());
					throw new RecordFileException(message);
				} else {
					String entryName = calculateRecordFileEntryName(fileAttribute);
					writeFile(zipOutputStream, file, entryName);
				}
			}
		}
	}

	public static String calculateRecordFileEntryName(FileAttribute fileAttribute) {
		FileAttributeDefinition fileAttributeDefinition = fileAttribute.getDefinition();
		String repositoryRelativePath = RecordFileManager.getRepositoryRelativePath(fileAttributeDefinition, ZIP_DIRECTORY_SEPARATOR, false);
		String relativePath = RECORD_FILE_DIRECTORY_NAME + ZIP_DIRECTORY_SEPARATOR + repositoryRelativePath;
		String entryName = relativePath + ZIP_DIRECTORY_SEPARATOR + fileAttribute.getFilename();
		return entryName;
	}

	private void writeFile(ZipOutputStream zipOutputStream, File file, String entryName) {
		try {
			ZipEntry entry = new ZipEntry(entryName);
			zipOutputStream.putNextEntry(entry);
			IOUtils.copy(new FileInputStream(file), zipOutputStream);
			zipOutputStream.closeEntry();
			zipOutputStream.flush();
		} catch (IOException e) {
			LOG.error(String.format("Error writing record file (fileName: %s)", entryName));
		}
	}

	public boolean isIncludeIdm() {
		return includeIdm;
	}

	public void setIncludeIdm(boolean includeIdm) {
		this.includeIdm = includeIdm;
	}
	
	public CollectSurvey getSurvey() {
		return survey;
	}
	
	public void setSurvey(CollectSurvey survey) {
		this.survey = survey;
	}
	
	public String getRootEntityName() {
		return rootEntityName;
	}
	
	public void setRootEntityName(String rootEntityName) {
		this.rootEntityName = rootEntityName;
	}
	
	public Step[] getSteps() {
		return steps;
	}
	
	public void setSteps(Step[] steps) {
		this.steps = steps;
	}
	
	public File getOutputFile() {
		return outputFile;
	}
	
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public Date getModifiedSince() {
		return modifiedSince;
	}

	public void setModifiedSince(Date modifiedSince) {
		this.modifiedSince = modifiedSince;
	}
	
}
