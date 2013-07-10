/**
 * 
 */
package org.openforis.collect.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectJoinStep;
import org.jooq.TableField;
import org.openforis.collect.model.NameValueEntry;
import org.openforis.collect.persistence.jooq.DialectAwareJooqFactory;
import org.openforis.collect.persistence.jooq.JooqDaoSupport;
import org.openforis.collect.persistence.jooq.tables.Lookup;
import org.openforis.collect.persistence.jooq.tables.records.LookupRecord;
import org.openforis.commons.collection.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author M. Togna
 * 
 */
@Transactional
public class DynamicTableDao extends JooqDaoSupport {

	@Deprecated
	@Transactional
	public Object load(String table, String column, Object... keys) {
		NameValueEntry[] filters = NameValueEntry.fromKeyValuePairs(keys);
		return loadValue(table, column, filters);
	}

	@Transactional
	public Object loadValue(String table, String column, NameValueEntry... filters) {
		Map<String, String> row = loadRow(table, filters);
		return row == null ? null: row.get(column);
	}

	@Transactional
	public Map<String, String> loadRow(String table, NameValueEntry[] filters) {
		List<Map<String, String>> rows = loadRows(table, filters);
		if ( rows == null || rows.isEmpty()) {
			return null;
		} else {
			return rows.get(0);
		}
	}

	@Transactional
	public List<Map<String, String>> loadRows(String table, NameValueEntry[] filters) {
		return loadRows(table, filters, (String[]) null);
	}
	
	@Transactional
	public List<Map<String, String>> loadRows(String table, NameValueEntry[] filters, String[] notNullColumns) {
		Lookup lookupTable = Lookup.getInstance(table);
		initTable(table);
		DialectAwareJooqFactory factory = getJooqFactory();
		List<Field<?>> fields = lookupTable.getFields();
		SelectJoinStep select = factory.select(fields).from(lookupTable);
		
		addFilterConditions(lookupTable, select, filters);
		addNotNullConditions(lookupTable, select, notNullColumns);
		
		List<Map<String, String>> result = new ArrayList<Map<String,String>>();
		Result<Record> selectResult = select.fetch();
		if ( selectResult != null ) {
			for (Record record : selectResult) {
				Map<String, String> rowMap = parseRecord(record, fields);
				result.add(rowMap);
			}
		}
		return CollectionUtils.unmodifiableList(result);
	}

	@Transactional
	public boolean exists(String table, NameValueEntry[] filters, String[] notNullColumns) {
		Lookup lookupTable = Lookup.getInstance(table);
		initTable(table);
		DialectAwareJooqFactory factory = getJooqFactory();
		SelectJoinStep select = factory.selectCount().from(lookupTable);
		addFilterConditions(lookupTable, select, filters);
		addNotNullConditions(lookupTable, select, notNullColumns);
		Record record = select.fetchOne();
		Integer count = (Integer) record.getValue(0);
		return count > 0;
	}
	
	protected void addFilterConditions(Lookup lookupTable,
			SelectJoinStep select, NameValueEntry[] filters) {
		for (NameValueEntry filter : filters) {
			String colName = filter.getKey();
			@SuppressWarnings("unchecked")
			TableField<LookupRecord, Object> tableField = (TableField<LookupRecord, Object>) lookupTable.getField(colName);
			if ( tableField != null ) {
				Object filterValue = filter.getValue();
				Condition condition;
				if ( tableField.getType() == String.class && 
						(filterValue == null || filterValue instanceof String && StringUtils.isEmpty((String) filterValue)) ) {
					condition = tableField.isNull().or(tableField.trim().equal(""));
				} else if ( filterValue == null ) {
					condition = tableField.isNull();
				} else {
					condition = tableField.equal(filterValue);
				}
				select.where(condition);
			} else {
				logger.warn("Filter not applied: " + filter);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void addNotNullConditions(Lookup lookupTable, SelectJoinStep select, String[] columns) {
		if ( columns != null ) {
			for (String colName : columns) {
				Field<?> tableField = lookupTable.getField(colName);
				if ( tableField != null ) {
					select.where(tableField.isNotNull().and(((Field<String>) tableField).notEqual("")));
				} else {
					logger.warn("Not null filter not applied on column: " + colName);
				}
			}
		}
	}

	protected void initTable(String table) {
		Lookup lookupTable = Lookup.getInstance(table);
		Collection<Map<String, ?>> colsMetadata = getColumnsMetadata(table);
		for (Map<String, ?> colMetadata : colsMetadata) {
			String colName = (String) colMetadata.get("COLUMN_NAME");
			if ( lookupTable.getField(colName) == null ) {
				Integer dataType = (Integer) colMetadata.get("DATA_TYPE");
				lookupTable.createField(colName, dataType);
			}
			
		}
	}
	
	private List<Map<String, ?>> getColumnsMetadata(String table) {
		List<Map<String, ?>> result = new ArrayList<Map<String,?>>();
		try { 
			DialectAwareJooqFactory factory = getJooqFactory();
			Connection connection = factory.getConnection();
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet columnRs = metaData.getColumns(null, null, table, null);
			while (columnRs.next()) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("COLUMN_NAME", columnRs.getString("COLUMN_NAME"));
				map.put("DATA_TYPE", columnRs.getInt("DATA_TYPE"));
				result.add(map);
			}
		} catch(SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	protected Map<String, String> parseRecord(Record record, List<Field<?>> fields) {
		Map<String, String> rowMap = new HashMap<String, String>();
		for (Field<?> field : fields) {
			String key = field.getName();
			String value = record.getValueAsString(key);
			rowMap.put(key, value);
		}
		return rowMap;
	}
	
}
