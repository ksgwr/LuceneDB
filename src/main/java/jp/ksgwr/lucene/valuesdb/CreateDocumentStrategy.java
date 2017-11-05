package jp.ksgwr.lucene.valuesdb;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;

/**
 * create document strategy interface
 *
 * @author ksgwr
 *
 */
public interface CreateDocumentStrategy {

	/**
	 * has header
	 * @return if true, skip first line, and set headerRawFields
	 */
	public boolean hasHeader();

	/**
	 * create header fields
	 * @param headerRawFields if hasHeader is false, set null
	 * @return Header Fields, this use search fields alias
	 */
	public String[] createHeaderFields(String[] headerRawFields);

	/**
	 * create field type
	 * @param firstFields first line data, can use guess field type
	 * @return Lucene Field Type Array
	 */
	public Class<? extends Field>[] createFieldsType(String[] firstFields);

	/**
	 * create stores
	 * @param fieldsLength array length
	 * @return stores fields
	 */
	public Store[] createStores(int fieldsLength);
}
