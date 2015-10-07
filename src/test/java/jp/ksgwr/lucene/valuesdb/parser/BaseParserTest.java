package jp.ksgwr.lucene.valuesdb.parser;

import static org.junit.Assert.*;

import org.junit.Test;

public class BaseParserTest {

	@Test
	public void parseTest() {
		BaseParser parser = new BaseParser();
		parser.delimiter = ',';
		parser.startQuote = new char[]{'\'', '"'};
		parser.endQuote = new char[]{'\'', '"'};
		parser.escape = '\\';

		String[] fields;

		// simple
		fields = parser.parse("a,b,c,d");
		assertArrayEquals(new String[]{"a", "b", "c", "d"}, fields);

		// 1 quote
		fields = parser.parse("a,'b',c,d");
		assertArrayEquals(new String[]{"a", "b", "c", "d"}, fields);

		// 1 different quote
		fields = parser.parse("a,\"b\",c,d");
		assertArrayEquals(new String[]{"a", "b", "c", "d"}, fields);

		// 2 empty field
		fields = parser.parse("a,,,d");
		assertArrayEquals(new String[]{"a", "", "", "d"}, fields);

		// empty last field
		fields = parser.parse("a,");
		assertArrayEquals(new String[]{"a", ""}, fields);

		// 1 quote include delimiter
		fields = parser.parse("a,'b,b',c,d");
		assertArrayEquals(new String[]{"a", "b,b", "c", "d"}, fields);

		// 1 invalid quote field (but not wrong)
		fields = parser.parse("a, 'b' ,c");
		assertArrayEquals(new String[]{"a", " 'b' ", "c"}, fields);

	}

	@Test
	public void parseNoQuoteOptionTest() {
		BaseParser parser = new BaseParser();
		parser.delimiter = ',';
		parser.startQuote = new char[0];
		parser.endQuote = new char[0];

		String[] fields;

		// 1 quote
		fields = parser.parse("a,'b',c,d");
		assertArrayEquals(new String[]{"a", "'b'", "c", "d"}, fields);

	}

	@Test
	public void parseWrongTest() {
		BaseParser parser = new BaseParser();
		parser.delimiter = ',';
		parser.startQuote = new char[]{'\'', '"'};
		parser.endQuote = new char[]{'\'', '"'};
		parser.escape = '\\';

		String[] fields;

		// not exists end quote
		fields = parser.parse("a,'b,c,d");
		assertArrayEquals(new String[]{"a", "b,c,d"}, fields);

		// not exists delimiter
		fields = parser.parse("a,'b'c,d");
		assertArrayEquals(new String[]{"a", "b", "", "d"}, fields);

		// not exists delimiter
		fields = parser.parse("a,b,'");
		assertArrayEquals(new String[]{"a", "b", ""}, fields);

		// not exists delimiter
		fields = parser.parse("a,b,'c' ");
		assertArrayEquals(new String[]{"a", "b", "c", ""}, fields);

		// different quote
		fields = parser.parse("a,'b\",'c'");
		assertArrayEquals(new String[]{"a", "b\",", ""}, fields);
	}

	@Test
	public void parseExceptionTest() {
		BaseParser parser = new BaseParser();
		parser.delimiter = ',';
		parser.startQuote = new char[]{'\'', '"'};
		parser.endQuote = new char[]{'\'', '"'};
		parser.escape = '\\';

		// if error, throws RuntimeException
		parser.strict = true;

		String[] fields;

		try {
			// not exists end quote
			fields = parser.parse("a,'b,c,d");
			assertArrayEquals(new String[] { "a", "b,c,d" }, fields);
			fail();
		} catch (RuntimeException e) {

		}

		try {
			// not exists delimiter
			fields = parser.parse("a,'b'c,d");
			assertArrayEquals(new String[] { "a", "b", "", "d" }, fields);
			fail();
		} catch (RuntimeException e) {

		}

		try {
			// not exists delimiter
			fields = parser.parse("a,b,'");
			assertArrayEquals(new String[] { "a", "b", "" }, fields);
			fail();
		} catch (RuntimeException e) {

		}
		try {
			// not exists delimiter
			fields = parser.parse("a,b,'c' ");
			assertArrayEquals(new String[] { "a", "b", "c", "" }, fields);
			fail();
		} catch (RuntimeException e) {

		}
		try {
			// different quote
			fields = parser.parse("a,'b\",'c'");
			assertArrayEquals(new String[] { "a", "b\",", "" }, fields);
			fail();
		} catch (RuntimeException e) {

		}
	}

	@Test
	public void writeTest() {
		BaseParser parser = new BaseParser();
		parser.delimiter = ',';
		parser.startQuote = new char[]{'\'', '"'};
		parser.endQuote = new char[]{'\'', '"'};
		parser.escape = '\\';
		parser.autoEscape = true;

		String[] fields = new String[] {"a", "b'b", "c,c", "d"};
		String actual = parser.writeDocument(fields);
		assertEquals("a,b'b,'c\\,c',d", actual);
	}
}
