package jp.ksgwr.lucene.valuesdb;

/**
 * write document strategy
 * @author ksgwr
 *
 */
public interface WriteDocumentStrategy {

	/**
	 * write document
	 * @param fields field values
	 * @return line
	 */
	public String writeDocument(String[] fields);
}
