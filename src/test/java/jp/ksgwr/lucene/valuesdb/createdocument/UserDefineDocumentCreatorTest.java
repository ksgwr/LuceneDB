package jp.ksgwr.lucene.valuesdb.createdocument;

import static org.junit.Assert.*;

import java.awt.TextField;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import jp.ksgwr.lucene.valuesdb.LuceneValuesDB;
import jp.ksgwr.lucene.valuesdb.parser.CSVParser;

import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.junit.Test;

public class UserDefineDocumentCreatorTest {

	@Test
	public void simpleTest() throws IOException {
		LuceneValuesDB valuesDB = new LuceneValuesDB();
		URL testPath = LuceneValuesDB.class.getResource("test.csv");

		@SuppressWarnings("unchecked")
		UserDefineDocumentCreator creator = new UserDefineDocumentCreator(new Class[] {
				IntField.class,
				StringField.class,
				FloatField.class,
				TextField.class
		}, new String[] {
				"docNum",
				"docType",
				"score",
				"text"
		});

		valuesDB.open(new File(testPath.getFile()), new CSVParser(), creator);

		assertEquals(1, valuesDB.search("docNum", 0).length);
		assertEquals(1, valuesDB.search("docType", "a").length);
		assertEquals(2, valuesDB.search("score", "0.1").length);
		assertEquals(1, valuesDB.search("text", "this is a pen").length);
	}
}
