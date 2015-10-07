package jp.ksgwr.lucene.valuesdb.createdocument;

import jp.ksgwr.lucene.valuesdb.CreateDocumentStrategy;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;

/**
 * user define document creator
 * @author ksgwr
 *
 */
public class UserDefineDocumentCreator implements CreateDocumentStrategy {

	/** if true, read header field name */
	protected boolean hasHeader;

	/** field types */
	protected Class<? extends Field>[] fieldTypes;

	/** header fields name */
	protected String[] headerFields;

	/**
	 * constructor
	 * @param fieldTypes field types
	 */
	public UserDefineDocumentCreator(Class<? extends Field>[] fieldTypes) {
		this(fieldTypes, false);
	}

	/**
	 * constructor
	 * @param fieldTypes field types
	 * @param hasHeader header fields name
	 */
	public UserDefineDocumentCreator(Class<? extends Field>[] fieldTypes, boolean hasHeader) {
		this.hasHeader = hasHeader;
		this.fieldTypes = fieldTypes;
		this.headerFields = null;
	}

	/**
	 * constructor
	 * @param fieldTypes field types
	 * @param headerFields header fields name
	 */
	public UserDefineDocumentCreator(Class<? extends Field>[] fieldTypes, String[] headerFields) {
		this.fieldTypes = fieldTypes;
		this.headerFields = headerFields;
	}

	@Override
	public boolean hasHeader() {
		return false;
	}

	@Override
	public String[] createHeaderFields(String[] headerRawFields) {
		if (headerFields != null) {
			return headerFields;
		}
		return headerRawFields;
	}

	@Override
	public Class<? extends Field>[] createFieldsType(String[] firstFields) {
		return fieldTypes;
	}

	@Override
	public Store[] createStores(int fieldsLength) {
		Store[] stores = new Store[fieldsLength];
		for(int i=0;i<fieldsLength;i++) {
			stores[i] = Store.YES;
		}
		return stores;
	}


}
