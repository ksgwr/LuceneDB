package jp.ksgwr.lucene.valuesdb.createdocument;

import jp.ksgwr.lucene.valuesdb.CreateDocumentStrategy;

import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;

/**
 * auto detect document creator
 * detect Float, Double, Integer, Float, Text
 *
 * @author ksgwr
 *
 */
public class AutoDetectDocumentCreator implements CreateDocumentStrategy {

	/** if true, read header field name */
	protected boolean hasHeader;

	/**
	 * constructor
	 */
	public AutoDetectDocumentCreator() {
		this(false);
	}

	/**
	 * constructor
	 * @param hasHeader if true, read header field name
	 */
	public AutoDetectDocumentCreator(boolean hasHeader) {
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
			classes[i] = detectFieldType(firstFields[i]);
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

	/**
	 * detect Float, Double, Integer, Long, Text
	 * @param value field value
	 * @return detect type
	 */
	public Class<? extends Field> detectFieldType(String value) {
		if (value.indexOf('.')>=0) {
			try{
				Float.valueOf(value);
				return FloatField.class;
			} catch(NumberFormatException e) {
			}
			try {
				Double.valueOf(value);
				return DoubleField.class;
			} catch(NumberFormatException e) {
			}
		} else {
			// no detect short
			try{
				Integer.valueOf(value);
				return IntField.class;
			} catch(NumberFormatException e) {
			}
			try{
				Long.valueOf(value);
				return LongField.class;
			} catch(NumberFormatException e) {
			}
		}
		return TextField.class;
	}
}
