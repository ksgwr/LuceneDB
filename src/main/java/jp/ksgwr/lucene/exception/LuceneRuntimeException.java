package jp.ksgwr.lucene.exception;

/**
 * RuntimeException wrapping LuceneException
 * @author ksgwr
 *
 */
public class LuceneRuntimeException extends RuntimeException {

	/**
	 * constructor
	 * @param e exception
	 */
	public LuceneRuntimeException(Exception e) {
		super(e);
	}

}
