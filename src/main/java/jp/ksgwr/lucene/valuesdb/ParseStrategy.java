package jp.ksgwr.lucene.valuesdb;

/**
 * Parse Strategy interface
 * @author ksgwr
 *
 */
public interface ParseStrategy {

	/**
	 * parse line
	 * @param line line
	 * @return parsed fields
	 */
	String[] parse(String line);
}
