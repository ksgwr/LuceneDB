package jp.ksgwr.lucene.valuesdb;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.ksgwr.lucene.valuesdb.createdocument.AutoDetectDocumentCreator;
import jp.ksgwr.lucene.valuesdb.parser.CSVParser;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LuceneValuesDBTest {

	private LuceneValuesDB valuesDB;

	@Before
	public void setup() throws IOException {
		valuesDB = new LuceneValuesDB();
		URL testPath = LuceneValuesDB.class.getResource("test.csv");
		valuesDB.open(new File(testPath.getFile()), new CSVParser(), new AutoDetectDocumentCreator());
	}

	@After
	public void cleanup() throws IOException {
		valuesDB.close();
	}

	@Test
	public void searchIntFieldTest() throws IOException, ParseException {
		Document[] docs = valuesDB.search(0, 1);
		List<List<String>> actual = valuesDB.convertDocumentList(docs);
 		List<List<String>> expected = convertList(new String[][]{
				{"1","b","0.1","this is a cat"}
		});

		assertEquals(expected, actual);
	}

	@Test
	public void searchDoubleFieldTest() throws IOException, ParseException {
		Document[] docs = valuesDB.search(2, 0.1);
		List<List<String>> actual = valuesDB.convertDocumentList(docs);
 		List<List<String>> expected = convertList(new String[][]{
 				{"0","a","0.1","this is a pen"},
				{"1","b","0.1","this is a cat"}
		});

		assertEquals(expected, actual);
	}

	@Test
	public void searchPartialMatchTest() throws IOException, ParseException {
		Document[] docs = valuesDB.search("3", "p");
		List<List<String>> actual = valuesDB.convertDocumentList(docs);
 		List<List<String>> expected = convertList(new String[][]{
 				{"0","a","0.1","this is a pen"},
 				{"2","c","0.2","p"}
		});

		assertEquals(expected, actual);
	}

	@Test
	public void searchExactMatchTest() throws IOException, ParseException {
		Document[] docs = valuesDB.exactSearch(3, "cat");
		List<List<String>> actual = valuesDB.convertDocumentList(docs);
 		List<List<String>> expected = convertList(new String[][]{
 				{"3","d","-0.3","cat"}
		});

		assertEquals(expected, actual);
	}

	@Test
	public void searchAndTest() throws IOException, ParseException {
		QueryParser parser = valuesDB.createQueryParser();
		Query query = parser.parse("3:\"this\" AND 3:\"pen\"");
		Document[] docs = valuesDB.search(query);
		List<List<String>> actual = valuesDB.convertDocumentList(docs);
 		List<List<String>> expected = convertList(new String[][]{
 				{"0","a","0.1","this is a pen"}
		});
		assertEquals(expected, actual);
	}

	private List<List<String>> convertList(String[][] matrix) {
		List<List<String>> result = new ArrayList<List<String>>(matrix.length);
		for(String[] colmun:matrix) {
			result.add(Arrays.asList(colmun));
		}
		return result;
	}
}
