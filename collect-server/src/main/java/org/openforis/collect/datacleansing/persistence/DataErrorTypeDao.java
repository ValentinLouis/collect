package org.openforis.collect.datacleansing.persistence;

import static org.openforis.collect.persistence.jooq.Sequences.OFC_DATA_ERROR_TYPE_ID_SEQ;
import static org.openforis.collect.persistence.jooq.tables.OfcDataErrorType.OFC_DATA_ERROR_TYPE;

import java.sql.Connection;
import java.util.List;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.ResultQuery;
import org.jooq.Select;
import org.jooq.StoreQuery;
import org.openforis.collect.datacleansing.DataErrorType;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.collect.persistence.jooq.SurveyObjectMappingDSLContext;
import org.openforis.collect.persistence.jooq.SurveyObjectMappingJooqDaoSupport;
import org.openforis.collect.persistence.jooq.tables.records.OfcDataErrorTypeRecord;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author S. Ricci
 */
@Transactional
public class DataErrorTypeDao extends SurveyObjectMappingJooqDaoSupport<DataErrorType, DataErrorTypeDao.JooqDSLContext> {

	public DataErrorTypeDao() {
		super(DataErrorTypeDao.JooqDSLContext.class);
	}

	public DataErrorType loadById(CollectSurvey survey, int id) {
		JooqDSLContext dsl = dsl(survey);
		ResultQuery<?> selectQuery = dsl.selectByIdQuery(id);
		Record r = selectQuery.fetchOne();
		return r == null ? null : dsl.fromRecord(r);
	}

	public List<DataErrorType> loadBySurvey(CollectSurvey survey) {
		JooqDSLContext dsl = dsl(survey);
		Select<OfcDataErrorTypeRecord> select = dsl.selectFrom(OFC_DATA_ERROR_TYPE)
			.where(OFC_DATA_ERROR_TYPE.SURVEY_ID.eq(survey.getId()));
		Result<OfcDataErrorTypeRecord> result = select.fetch();
		return dsl.fromResult(result);
	}

	protected static class JooqDSLContext extends SurveyObjectMappingDSLContext<DataErrorType> {

		private static final long serialVersionUID = 1L;
		
		public JooqDSLContext(Connection connection, CollectSurvey survey) {
			super(connection, OFC_DATA_ERROR_TYPE.ID, OFC_DATA_ERROR_TYPE_ID_SEQ, DataErrorType.class, survey);
		}
		
		@Override
		protected DataErrorType newEntity() {
			return new DataErrorType(getSurvey());
		}
		
		@Override
		protected void fromRecord(Record r, DataErrorType object) {
			object.setCode(r.getValue(OFC_DATA_ERROR_TYPE.CODE));
			object.setDescription(r.getValue(OFC_DATA_ERROR_TYPE.DESCRIPTION));
			object.setLabel(r.getValue(OFC_DATA_ERROR_TYPE.LABEL));
		}
		
		@Override
		protected void fromObject(DataErrorType object, StoreQuery<?> q) {
			q.addValue(OFC_DATA_ERROR_TYPE.CODE, object.getCode());
			q.addValue(OFC_DATA_ERROR_TYPE.DESCRIPTION, object.getDescription());
			q.addValue(OFC_DATA_ERROR_TYPE.LABEL, object.getLabel());
			q.addValue(OFC_DATA_ERROR_TYPE.SURVEY_ID, object.getSurvey().getId());
		}

		@Override
		protected void setId(DataErrorType entity, int id) {
			entity.setId(id);
		}

		@Override
		protected Integer getId(DataErrorType entity) {
			return entity.getId();
		}
		
	}
}
