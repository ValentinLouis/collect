package org.openforis.collect.util {
	
	import org.granite.collections.BasicMap;
	import org.openforis.collect.metamodel.NodeDefinitionSummary;
	import org.openforis.collect.metamodel.proxy.AttributeDefaultProxy;
	import org.openforis.collect.metamodel.proxy.AttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.BooleanAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.CodeAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.CodeListItemProxy;
	import org.openforis.collect.metamodel.proxy.CodeListLabelProxy;
	import org.openforis.collect.metamodel.proxy.CodeListLabelProxy$Type;
	import org.openforis.collect.metamodel.proxy.CodeListLevelProxy;
	import org.openforis.collect.metamodel.proxy.CodeListProxy;
	import org.openforis.collect.metamodel.proxy.CodeListProxy$CodeScope;
	import org.openforis.collect.metamodel.proxy.CodeListProxy$CodeType;
	import org.openforis.collect.metamodel.proxy.CoordinateAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.DateAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.EntityDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.FileAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.LanguageSpecificTextProxy;
	import org.openforis.collect.metamodel.proxy.ModelVersionProxy;
	import org.openforis.collect.metamodel.proxy.NodeLabelProxy;
	import org.openforis.collect.metamodel.proxy.NodeLabelProxy$Type;
	import org.openforis.collect.metamodel.proxy.PrecisionProxy;
	import org.openforis.collect.metamodel.proxy.PromptProxy;
	import org.openforis.collect.metamodel.proxy.PromptProxy$Type;
	import org.openforis.collect.metamodel.proxy.RangeAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.SchemaProxy;
	import org.openforis.collect.metamodel.proxy.SpatialReferenceSystemProxy;
	import org.openforis.collect.metamodel.proxy.SurveyProxy;
	import org.openforis.collect.metamodel.proxy.TaxonAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.TaxonSummariesProxy;
	import org.openforis.collect.metamodel.proxy.TaxonSummaryProxy;
	import org.openforis.collect.metamodel.proxy.TextAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.TextAttributeDefinitionProxy$Type;
	import org.openforis.collect.metamodel.proxy.TimeAttributeDefinitionProxy;
	import org.openforis.collect.metamodel.proxy.UIOptionsProxy;
	import org.openforis.collect.metamodel.proxy.UnitProxy;
	import org.openforis.collect.metamodel.ui.proxy.ColumnGroupProxy;
	import org.openforis.collect.metamodel.ui.proxy.ColumnProxy;
	import org.openforis.collect.metamodel.ui.proxy.FormProxy;
	import org.openforis.collect.metamodel.ui.proxy.FormSectionProxy;
	import org.openforis.collect.metamodel.ui.proxy.FormSetProxy;
	import org.openforis.collect.metamodel.ui.proxy.TableProxy;
	import org.openforis.collect.model.SurveySummary;
	import org.openforis.collect.model.proxy.AttributeAddResponseProxy;
	import org.openforis.collect.model.proxy.AttributeProxy;
	import org.openforis.collect.model.proxy.AttributeUpdateResponseProxy;
	import org.openforis.collect.model.proxy.CodeAttributeProxy;
	import org.openforis.collect.model.proxy.CodeProxy;
	import org.openforis.collect.model.proxy.CoordinateProxy;
	import org.openforis.collect.model.proxy.DateProxy;
	import org.openforis.collect.model.proxy.EntityAddResponseProxy;
	import org.openforis.collect.model.proxy.EntityProxy;
	import org.openforis.collect.model.proxy.EntityUpdateResponseProxy;
	import org.openforis.collect.model.proxy.FieldProxy;
	import org.openforis.collect.model.proxy.FileProxy;
	import org.openforis.collect.model.proxy.IntegerRangeProxy;
	import org.openforis.collect.model.proxy.NodeDeleteResponseProxy;
	import org.openforis.collect.model.proxy.NodeProxy;
	import org.openforis.collect.model.proxy.RealRangeProxy;
	import org.openforis.collect.model.proxy.RecordProxy;
	import org.openforis.collect.model.proxy.RecordUpdateRequestProxy;
	import org.openforis.collect.model.proxy.RecordUpdateRequestSetProxy;
	import org.openforis.collect.model.proxy.RecordUpdateResponseSetProxy;
	import org.openforis.collect.model.proxy.SamplingDesignItemProxy;
	import org.openforis.collect.model.proxy.SamplingDesignSummariesProxy;
	import org.openforis.collect.model.proxy.TaxonOccurrenceProxy;
	import org.openforis.collect.model.proxy.TaxonProxy;
	import org.openforis.collect.model.proxy.TaxonVernacularNameProxy;
	import org.openforis.collect.model.proxy.TaxonomyProxy;
	import org.openforis.collect.model.proxy.TimeProxy;
	import org.openforis.collect.model.proxy.ValidationResultProxy;
	import org.openforis.collect.model.proxy.ValidationResultsProxy;
	import org.openforis.collect.remoting.service.codelistimport.proxy.CodeListImportStatusProxy;
	import org.openforis.collect.remoting.service.dataexport.DataExportState;
	import org.openforis.collect.remoting.service.dataimport.DataImportStateProxy;
	import org.openforis.collect.remoting.service.dataimport.DataImportSummaryItemProxy;
	import org.openforis.collect.remoting.service.dataimport.DataImportSummaryProxy;
	import org.openforis.collect.remoting.service.dataimport.FileUnmarshallingErrorProxy;
	import org.openforis.collect.remoting.service.dataimport.NodeUnmarshallingErrorProxy;
	import org.openforis.collect.remoting.service.samplingdesignimport.proxy.SamplingDesignImportStatusProxy;
	import org.openforis.collect.remoting.service.speciesimport.proxy.SpeciesImportStatusProxy;
	import org.openforis.idm.metamodel.validation.ValidationResultFlag;

	/**
	 * 
	 * @author M. Togna
	 * @author S. Ricci
	 * 
	 */
	public class ModelClassInitializer {

		public static function init():void {
			var array:Array = [
				AttributeAddResponseProxy,
				AttributeDefaultProxy,
				AttributeDefinitionProxy,
				AttributeProxy,
				AttributeUpdateResponseProxy,
				BasicMap, 
				BooleanAttributeDefinitionProxy,
				CodeAttributeDefinitionProxy,
				CodeAttributeProxy,
				CodeListImportStatusProxy,
				CodeListItemProxy,
				CodeListLabelProxy,
				CodeListLabelProxy$Type,
				CodeListLevelProxy,
				CodeListProxy,
				CodeListProxy$CodeScope,
				CodeListProxy$CodeType,
				ColumnGroupProxy,
				ColumnProxy,
				CodeProxy,
				CoordinateAttributeDefinitionProxy,
				CoordinateProxy,
				DataExportState,
				DataImportStateProxy,
				DataImportSummaryProxy,
				DataImportSummaryItemProxy,
				DateAttributeDefinitionProxy,
				DateProxy,
				EntityAddResponseProxy,
				EntityDefinitionProxy,
				EntityUpdateResponseProxy,
				EntityProxy,
				FieldProxy,
				org.openforis.collect.metamodel.ui.proxy.FieldProxy,
				FileAttributeDefinitionProxy,
				FileUnmarshallingErrorProxy,
				FileProxy,
				FormProxy,
				FormSectionProxy,
				FormSetProxy,
				IntegerRangeProxy,
				LanguageSpecificTextProxy,
				ModelVersionProxy,
				NodeDefinitionSummary,
				NodeDeleteResponseProxy,
				NodeUnmarshallingErrorProxy,
				NodeLabelProxy,
				NodeLabelProxy$Type,
				NodeProxy,
				PrecisionProxy,
				PromptProxy,
				PromptProxy$Type,
				RangeAttributeDefinitionProxy,
				RealRangeProxy,
				RecordProxy,
				RecordUpdateRequestProxy,
				RecordUpdateRequestSetProxy,
				RecordUpdateResponseSetProxy,
				SamplingDesignImportStatusProxy,
				SamplingDesignItemProxy,
				SamplingDesignSummariesProxy,
				SchemaProxy,
				SpatialReferenceSystemProxy,
				SpeciesImportStatusProxy,
				SurveyProxy,
				SurveySummary,
				TableProxy,
				TaxonAttributeDefinitionProxy,
				TaxonOccurrenceProxy,
				TaxonomyProxy,
				TaxonProxy,
				TaxonSummariesProxy,
				TaxonSummaryProxy,
				TaxonVernacularNameProxy,
				TextAttributeDefinitionProxy,
				TextAttributeDefinitionProxy$Type,
				TimeAttributeDefinitionProxy,
				TimeProxy,
				UIOptionsProxy,
				UnitProxy,
				ValidationResultsProxy,
				ValidationResultProxy,
				ValidationResultFlag
			];
		}
		
	}
}