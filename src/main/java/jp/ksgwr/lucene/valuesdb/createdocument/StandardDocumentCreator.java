package jp.ksgwr.lucene.valuesdb.createdocument;

import jp.ksgwr.lucene.valuesdb.CreateDocumentStrategy;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;

/**
 * create document simple strategy, all field set TextField
 * @author ksgwr
 *
 */
public class StandardDocumentCreator implements CreateDocumentStrategy {

	protected boolean hasHeader;

	/**
	 * constructor
	 */
	public StandardDocumentCreator() {
		this(false);
	}

	/**
	 * constructor
	 * @param hasHeader if true, read header field name
	 */
	public StandardDocumentCreator(boolean hasHeader) {
		this.hasHeader = hasHeader;
	}

	@Override
	public boolean hasHeader() {
		return hasHeader;
	}

	@Override
	public String[] createHeaderFields(String[] headerRawFields) {
		return headerRawFields;
	}

	@Override
	public Class<? extends Field>[] createFieldsType(String[] firstFields) {
		@SuppressWarnings("unchecked")
		Class<? extends Field>[] classes = new Class[firstFields.length];
		for(int i=0;i<firstFields.length;i++) {
			classes[i] = TextField.class;
		}
		return classes;
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
