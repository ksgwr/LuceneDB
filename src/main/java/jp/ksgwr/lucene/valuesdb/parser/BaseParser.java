package jp.ksgwr.lucene.valuesdb.parser;

import java.text.ParseException;
import java.util.ArrayList;

import jp.ksgwr.lucene.valuesdb.ParseStrategy;
import jp.ksgwr.lucene.valuesdb.WriteDocumentStrategy;

public class BaseParser implements ParseStrategy, WriteDocumentStrategy {

	/** delimiter */
	protected char delimiter;

	/** escape (enable in quote) */
	protected char escape;

	/** start quote */
	protected char[] startQuote;

	/** end quote */
	protected char[] endQuote;

	/** check parse if error occurs, throws RuntimeException */
	protected boolean strict;

	/** if include escape char, escape this automatically */
	protected boolean autoEscape;

	/** default escape char */
	public final char DEFAULT_ESCAPE = '\\';

	/** default start quote char */
	public final char[] DEFAULT_START_QUOTE = new char[]{'\'', '"'};

	/** default end quote char */
	public final char[] DEFAULT_END_QUOTE = new char[]{'\'', '"'};

	/**
	 * constructor
	 */
	protected BaseParser(){
	}

	/**
	 * simple constructor
	 * @param delimiter
	 */
	public BaseParser(char delimiter) {
		this.delimiter = delimiter;
		// default setting
		this.escape = DEFAULT_ESCAPE;
		this.startQuote = DEFAULT_START_QUOTE;
		this.endQuote = DEFAULT_END_QUOTE;
		this.strict = true;
		this.autoEscape = true;
	}

	@Override
	public String[] parse(String str) {
		ArrayList<String> list = new ArrayList<String>();
		boolean quote = false;
		char c;
		int start=0;
		int quoteIndex = -1;
		ArrayList<String> escapes = new ArrayList<String>();
		for(int i=0;i<str.length();i++) {
			c = str.charAt(i);
			if(i==start && !quote && (quoteIndex=indexOf(startQuote, c)) >= 0) {
				quote = true;
				start = i + 1;
			}else if(quote && c==escape) {
				escapes.add(str.substring(start, i));
				start = i + 1;
				i++;
			}else if(quote && c==endQuote[quoteIndex]) {
				quote = false;
				if (escapes.size() > 0) {
					StringBuilder sb = new StringBuilder(str.length());
					for(String subStr:escapes) {
						sb.append(subStr);
					}
					sb.append(str.substring(start, i));
					list.add(sb.toString());
				} else {
					// no escape
					list.add(str.substring(start, i));
				}
				//skip delimiter
				i++;
				start = i + 1;
				if (strict) {
					//check delimiter
					c = str.charAt(i);
					if (c!=delimiter) {
						throw new RuntimeException(new ParseException("not found delimiter after end quote.offset"+i+"="+c+":"+str, i));
					}
				}
			}else if(!quote && c==delimiter) {
				list.add(str.substring(start, i));
				start = i + 1;
			}
		}
		if (start <= str.length()) {
			list.add(str.substring(start));
		}
		if (strict) {
			if(quote) {
				throw new RuntimeException(new ParseException("not found end quote."+str, str.length()-1));
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * search index number
	 * @param chars search target
	 * @param x search char
	 * @return if find, return index else return -1
	 */
	private static final int indexOf(char[] chars, char x) {
		for(int i=0;i<chars.length;i++) {
			if (chars[i]==x) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public String writeDocument(String[] fields) {
		StringBuilder sb = new StringBuilder();
		for(String field:fields) {
			if(autoEscape) {
				field = escapeIfIncludeDelimiter(field);
			}
			sb.append(field);
			sb.append(delimiter);
		}
		if(fields.length>0) {
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}

	/**
	 * if include delimiter, escape its delimiter and wrap quote.
	 * @param str string
	 * @return escaped string
	 */
	private String escapeIfIncludeDelimiter(String str) {
		boolean isEscaped = false;
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<str.length();i++) {
			char c = str.charAt(i);
			if(c==delimiter) {
				sb.append(escape);
				isEscaped = true;
			}
			sb.append(c);
		}
		if(isEscaped) {
			return startQuote[0] + sb.toString() + endQuote[0];
		} else {
			return str;
		}
	}

}
