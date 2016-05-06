package org.openforis.collect.designer.form.validator;

import static org.openforis.collect.designer.form.SurveyFileFormObject.FILENAME_FIELD_NAME;
import static org.openforis.collect.designer.form.SurveyFileFormObject.TYPE_FIELD_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.openforis.collect.designer.viewmodel.SurveyFileVM;
import org.openforis.collect.manager.SurveyManager;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.model.SurveyFile;
import org.openforis.collect.model.SurveyFile.SurveyFileType;
import org.zkoss.bind.ValidationContext;
import org.zkoss.util.resource.Labels;

/**
 * 
 * @author S. Ricci
 *
 */
public class SurveyFileFormValidator extends FormValidator {
	
	protected static final String SURVEY_MANAGER_ARG = "surveyManager";
	
	private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[\\w-]+\\.[\\w-]+$");

	private static final Set<String> RESERVED_FILENAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			SurveyFileType.COLLECT_EARTH_AREA_PER_ATTRIBUTE.getFixedFilename(), "balloon.html", "collectEarthCubes.xml.fmt", "kml_template.fmt", "placemark.idm.xml",
			"project_definition.properties", "README.txt", "test_plots.ced", "earthFiles",
			"data", "files", "sampling_design", "species", "idml.xml", "info.properties")));

//	private static final String[] SUPPORTED_CE_BOTTOM_BANNER_LOGO_FILE_EXTENSIONS = new String[]{"jpg", "jpeg"};

	@Override
	protected void internalValidate(ValidationContext ctx) {
		if (validateTypeUniqueness(ctx)) {
			if (validateFilename(ctx)) {
				validateFilenameUniqueness(ctx);
			}
		}
	}

	private boolean validateTypeUniqueness(ValidationContext ctx) {
		List<SurveyFile> otherSurveyFiles = loadSurveyFilesDifferentFromThis(ctx);
		String typeName = getValue(ctx, TYPE_FIELD_NAME);
		SurveyFileType type = SurveyFileType.valueOf(typeName);
		switch (type) {
		case COLLECT_EARTH_AREA_PER_ATTRIBUTE:
		case COLLECT_EARTH_EE_SCRIPT:
		case COLLECT_EARTH_BOTTOM_BANNER:
			if (containsFileWithType(otherSurveyFiles, type)) {
				addInvalidMessage(ctx, TYPE_FIELD_NAME, Labels.getLabel("survey.file.error.type_already_defined"));
				return false;
			} else {
				return true;
			}
		default:
			return true;
		}
	}

	private boolean validateFilename(ValidationContext ctx) {
		if (validateRequired(ctx, FILENAME_FIELD_NAME)) { 
			if (validateFilenamePattern(ctx)) {
				if (validateFilenameUniqueness(ctx)) {
//					String typeStr = getValue(ctx, SurveyFileFormObject.TYPE_FIELD_NAME);
//					SurveyFileType type = SurveyFileType.valueOf(typeStr);
//					if (type == SurveyFileType.COLLECT_EARTH_BOTTOM_BANNER_LOGO) {
//						return validateImageFilename(ctx);
//					} else {
						return true;
//					}
				}
			}
		}
		return false;
	}
	
//	private boolean validateImageFilename(ValidationContext ctx) {
//		String filename = getValue(ctx, SurveyFileFormObject.UPLOADED_FILE_NAME_FIELD);
//		String extension = FilenameUtils.getExtension(filename);
//		if (extension != null && Arrays.asList(SUPPORTED_CE_BOTTOM_BANNER_LOGO_FILE_EXTENSIONS).contains(extension.toLowerCase(Locale.ENGLISH))) {
//			return true;
//		} else {
//			addInvalidMessage(ctx, "survey.file.error.only_jpg_images_supported");
//			return false;
//		}
//	}

	private boolean validateFilenamePattern(ValidationContext ctx) {
		if (validateRegEx(ctx, VALID_FILENAME_PATTERN, FILENAME_FIELD_NAME, "survey.file.error.invalid_filename")) {
			String filename = getValue(ctx, FILENAME_FIELD_NAME);
			String typeName = getValue(ctx, TYPE_FIELD_NAME);
			SurveyFileType type = SurveyFileType.valueOf(typeName);
			switch (type) {
			case COLLECT_EARTH_AREA_PER_ATTRIBUTE:
				String expectedFileName = SurveyFileType.COLLECT_EARTH_AREA_PER_ATTRIBUTE.getFixedFilename();
				if (expectedFileName.equals(filename)) {
					return true;
				} else {
					String message = Labels.getLabel("survey.file.error.unexpected_filename",
							new String[] { expectedFileName });
					addInvalidMessage(ctx, message);
					return false;
				}
			default:
				if (RESERVED_FILENAMES.contains(filename)) {
					addInvalidMessage(ctx, FILENAME_FIELD_NAME, Labels.getLabel("survey.file.error.reserved_filename"));
					return false;
				} else {
					return true;
				}
			}
		} else {
			return false;
		}
	}

	private boolean validateFilenameUniqueness(ValidationContext ctx) {
		List<SurveyFile> otherSurveyFiles = loadSurveyFilesDifferentFromThis(ctx);
		String filename = getValue(ctx, FILENAME_FIELD_NAME);
		for (SurveyFile surveyFile : otherSurveyFiles) {
			if (surveyFile.getFilename().equals(filename)) {
				addInvalidMessage(ctx, FILENAME_FIELD_NAME, Labels.getLabel("survey.file.error.duplicate_filename"));
				return false;
			}
		}
		return true;
	}
	
	private List<SurveyFile> loadSurveyFilesDifferentFromThis(ValidationContext ctx) {
		List<SurveyFile> result = new ArrayList<SurveyFile>();
		SurveyFileVM vm = (SurveyFileVM) getVM(ctx);
		CollectSurvey survey = vm.getSurvey();
		SurveyManager surveyManager = getSurveyManager(ctx);
		List<SurveyFile> surveyFiles = surveyManager.loadSurveyFileSummaries(survey);
		SurveyFile editedSurveyFile = vm.getEditedItem();
		for (SurveyFile surveyFile : surveyFiles) {
			if (! surveyFile.getId().equals(editedSurveyFile.getId())) {
				result.add(surveyFile);
			}
		}
		return result;
	}
	
	private boolean containsFileWithType(List<SurveyFile> files, SurveyFileType type) {
		for (SurveyFile surveyFile : files) {
			if (type == surveyFile.getType()) {
				return true;
			}
		}
		return false;
	}
	
	protected SurveyManager getSurveyManager(ValidationContext ctx) {
		return (SurveyManager) ctx.getValidatorArg(SURVEY_MANAGER_ARG);
	}


}
