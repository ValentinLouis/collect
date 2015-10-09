/**
 * This class is generated by jOOQ
 */
package org.openforis.collect.persistence.jooq.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.4"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class OfcDataCleansingReportRecord extends org.jooq.impl.UpdatableRecordImpl<org.openforis.collect.persistence.jooq.tables.records.OfcDataCleansingReportRecord> implements org.jooq.Record10<java.lang.Integer, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.sql.Timestamp, java.lang.Integer, java.lang.Integer, java.sql.Timestamp, java.sql.Timestamp> {

	private static final long serialVersionUID = 789555592;

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.id</code>.
	 */
	public void setId(java.lang.Integer value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.id</code>.
	 */
	public java.lang.Integer getId() {
		return (java.lang.Integer) getValue(0);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.uuid</code>.
	 */
	public void setUuid(java.lang.String value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.uuid</code>.
	 */
	public java.lang.String getUuid() {
		return (java.lang.String) getValue(1);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.cleansing_chain_id</code>.
	 */
	public void setCleansingChainId(java.lang.Integer value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.cleansing_chain_id</code>.
	 */
	public java.lang.Integer getCleansingChainId() {
		return (java.lang.Integer) getValue(2);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.record_step</code>.
	 */
	public void setRecordStep(java.lang.Integer value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.record_step</code>.
	 */
	public java.lang.Integer getRecordStep() {
		return (java.lang.Integer) getValue(3);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.dataset_size</code>.
	 */
	public void setDatasetSize(java.lang.Integer value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.dataset_size</code>.
	 */
	public java.lang.Integer getDatasetSize() {
		return (java.lang.Integer) getValue(4);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.last_record_modified_date</code>.
	 */
	public void setLastRecordModifiedDate(java.sql.Timestamp value) {
		setValue(5, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.last_record_modified_date</code>.
	 */
	public java.sql.Timestamp getLastRecordModifiedDate() {
		return (java.sql.Timestamp) getValue(5);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.cleansed_records</code>.
	 */
	public void setCleansedRecords(java.lang.Integer value) {
		setValue(6, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.cleansed_records</code>.
	 */
	public java.lang.Integer getCleansedRecords() {
		return (java.lang.Integer) getValue(6);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.cleansed_nodes</code>.
	 */
	public void setCleansedNodes(java.lang.Integer value) {
		setValue(7, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.cleansed_nodes</code>.
	 */
	public java.lang.Integer getCleansedNodes() {
		return (java.lang.Integer) getValue(7);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.creation_date</code>.
	 */
	public void setCreationDate(java.sql.Timestamp value) {
		setValue(8, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.creation_date</code>.
	 */
	public java.sql.Timestamp getCreationDate() {
		return (java.sql.Timestamp) getValue(8);
	}

	/**
	 * Setter for <code>collect.ofc_data_cleansing_report.modified_date</code>.
	 */
	public void setModifiedDate(java.sql.Timestamp value) {
		setValue(9, value);
	}

	/**
	 * Getter for <code>collect.ofc_data_cleansing_report.modified_date</code>.
	 */
	public java.sql.Timestamp getModifiedDate() {
		return (java.sql.Timestamp) getValue(9);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Record1<java.lang.Integer> key() {
		return (org.jooq.Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record10 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row10<java.lang.Integer, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.sql.Timestamp, java.lang.Integer, java.lang.Integer, java.sql.Timestamp, java.sql.Timestamp> fieldsRow() {
		return (org.jooq.Row10) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row10<java.lang.Integer, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.sql.Timestamp, java.lang.Integer, java.lang.Integer, java.sql.Timestamp, java.sql.Timestamp> valuesRow() {
		return (org.jooq.Row10) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field1() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field2() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.UUID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field3() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.CLEANSING_CHAIN_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field4() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.RECORD_STEP;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field5() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.DATASET_SIZE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.sql.Timestamp> field6() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.LAST_RECORD_MODIFIED_DATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field7() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.CLEANSED_RECORDS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field8() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.CLEANSED_NODES;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.sql.Timestamp> field9() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.CREATION_DATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.sql.Timestamp> field10() {
		return org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT.MODIFIED_DATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value1() {
		return getId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value2() {
		return getUuid();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value3() {
		return getCleansingChainId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value4() {
		return getRecordStep();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value5() {
		return getDatasetSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.sql.Timestamp value6() {
		return getLastRecordModifiedDate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value7() {
		return getCleansedRecords();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value8() {
		return getCleansedNodes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.sql.Timestamp value9() {
		return getCreationDate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.sql.Timestamp value10() {
		return getModifiedDate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value1(java.lang.Integer value) {
		setId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value2(java.lang.String value) {
		setUuid(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value3(java.lang.Integer value) {
		setCleansingChainId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value4(java.lang.Integer value) {
		setRecordStep(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value5(java.lang.Integer value) {
		setDatasetSize(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value6(java.sql.Timestamp value) {
		setLastRecordModifiedDate(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value7(java.lang.Integer value) {
		setCleansedRecords(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value8(java.lang.Integer value) {
		setCleansedNodes(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value9(java.sql.Timestamp value) {
		setCreationDate(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord value10(java.sql.Timestamp value) {
		setModifiedDate(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfcDataCleansingReportRecord values(java.lang.Integer value1, java.lang.String value2, java.lang.Integer value3, java.lang.Integer value4, java.lang.Integer value5, java.sql.Timestamp value6, java.lang.Integer value7, java.lang.Integer value8, java.sql.Timestamp value9, java.sql.Timestamp value10) {
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached OfcDataCleansingReportRecord
	 */
	public OfcDataCleansingReportRecord() {
		super(org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT);
	}

	/**
	 * Create a detached, initialised OfcDataCleansingReportRecord
	 */
	public OfcDataCleansingReportRecord(java.lang.Integer id, java.lang.String uuid, java.lang.Integer cleansingChainId, java.lang.Integer recordStep, java.lang.Integer datasetSize, java.sql.Timestamp lastRecordModifiedDate, java.lang.Integer cleansedRecords, java.lang.Integer cleansedNodes, java.sql.Timestamp creationDate, java.sql.Timestamp modifiedDate) {
		super(org.openforis.collect.persistence.jooq.tables.OfcDataCleansingReport.OFC_DATA_CLEANSING_REPORT);

		setValue(0, id);
		setValue(1, uuid);
		setValue(2, cleansingChainId);
		setValue(3, recordStep);
		setValue(4, datasetSize);
		setValue(5, lastRecordModifiedDate);
		setValue(6, cleansedRecords);
		setValue(7, cleansedNodes);
		setValue(8, creationDate);
		setValue(9, modifiedDate);
	}
}
