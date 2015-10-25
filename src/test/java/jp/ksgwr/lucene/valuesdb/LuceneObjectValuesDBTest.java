package jp.ksgwr.lucene.valuesdb;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.ksgwr.lucene.valuesdb.annotation.TextFieldable;

import org.apache.lucene.document.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LuceneObjectValuesDBTest {

	private LuceneObjectValuesDB<Sample> valuesDB;

	@Before
	public void setup() throws IOException {
		valuesDB = new LuceneObjectValuesDB<Sample>(Sample.class);

		Sample sample = new Sample();
		sample.id = 1;
		sample.name = "cat";
		sample.names = "cat".toCharArray();
		sample.description = "this is a cat";
		sample.code = 123456L;
		sample.childIds = new int[]{3,4};
		sample.childs = new HashMap<Integer, String>() {{
			put(3, "black cat");
			put(4, "kuro");
		}};
		sample.flags = new ArrayList<Boolean>() {{
			add(true);
		}};

		valuesDB.addObject(sample);

		Sample sample2 = new Sample();
		sample2.id = 2;
		sample2.name = "dog";
		sample2.names = "dog".toCharArray();
		sample2.description = "this is a dog";
		sample2.code = 4567L;
		sample2.childIds = new int[]{4,5};
		sample2.childs = new HashMap<Integer, String>() {{
			put(4, "kuro");
			put(15, "doberman");
		}};
		sample2.flags = new ArrayList<Boolean>() {{
			add(false);
			add(false);
		}};

		valuesDB.addObject(sample2);

		valuesDB.commit();
	}

	@After
	public void cleanup() throws IOException {
		valuesDB.close();
	}

	@Test
	public void intSearchTest() throws IOException {
		Sample query = new Sample();
		query.id = 1;

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(1, (int)results.get(0).id);
		assertEquals("cat", results.get(0).name);
	}

	@Test
	public void exactSearchTest() throws IOException {
		Sample query = new Sample();
		query.name = "dog";

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(2, (int)results.get(0).id);
		assertEquals("dog", results.get(0).name);
	}

	@Test
	public void charArraySearchTest() throws IOException {
		Sample query = new Sample();
		query.names = "dog".toCharArray();

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(2, (int)results.get(0).id);
		assertEquals("dog", results.get(0).name);
	}

	@Test
	public void partialSearchTest() throws IOException {
		Sample query = new Sample();
		query.description = "dog";

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(2, (int)results.get(0).id);
		assertEquals("dog", results.get(0).name);
	}

	@Test
	public void partialLongSearchTest() throws IOException {
		Sample query = new Sample();
		query.code = 456L;

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(2, results.size());
		assertEquals(1, (int)results.get(0).id);
		assertEquals("cat", results.get(0).name);
		assertEquals(2, (int)results.get(1).id);
		assertEquals("dog", results.get(1).name);
	}

	@Test
	public void arraySearchTest() throws IOException {
		Sample query = new Sample();
		query.childIds = new int[] {4};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(2, results.size());
		assertEquals(1, (int)results.get(0).id);
		assertEquals("cat", results.get(0).name);
		assertEquals(2, (int)results.get(1).id);
		assertEquals("dog", results.get(1).name);
	}

	@Test
	public void arrayAndSearchTest() throws IOException {
		Sample query = new Sample();
		query.childIds = new int[] {4,5};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(2, (int)results.get(0).id);
		assertEquals("dog", results.get(0).name);
	}

	@Test
	public void mapKeySearchTest() throws IOException {
		Sample query = new Sample();
		query.childs = new HashMap<Integer, String>() {{
			put(15, null);
		}};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(2, (int)results.get(0).id);
		assertEquals("dog", results.get(0).name);
	}

	@Test
	public void mapKeyNotFoundSearchTest() throws IOException {
		Sample query = new Sample();
		query.childs = new HashMap<Integer, String>() {{
			put(5, null);
		}};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(0, results.size());
	}

	@Test
	public void mapValuePartialSearchTest() throws IOException {
		Sample query = new Sample();
		query.childs = new HashMap<Integer, String>() {{
			put(null, "black");
		}};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(1, (int)results.get(0).id);
		assertEquals("cat", results.get(0).name);
	}

	@Test
	public void listSearchTest() throws IOException {
		Sample query = new Sample();
		query.flags = new ArrayList<Boolean>() {{
			add(false);
		}};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(2, (int)results.get(0).id);
		assertEquals("dog", results.get(0).name);
	}

	@Test
	public void orSearchTest() throws IOException {
		Sample subQuery1 = new Sample();
		subQuery1.id = 1;
		Sample subQuery2 = new Sample();
		subQuery2.id = 2;
		Sample[] query = new Sample[]{
			subQuery1, subQuery2
		};

		Document[] docs = valuesDB.search(query);
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(2, results.size());
		assertEquals(1, (int)results.get(0).id);
		assertEquals("cat", results.get(0).name);
		assertEquals(2, (int)results.get(1).id);
		assertEquals("dog", results.get(1).name);
	}

	/**
	 * sample pojo class
	 *
	 * @author ksgwr
	 *
	 */
	public static class Sample {
		public Integer id;

		public String name;

		public char[] names;

		@TextFieldable
		public String description;

		@TextFieldable
		public Long code;

		public int childIds[];

		@TextFieldable(key=false)
		public Map<Integer, String> childs;

		public List<Boolean> flags;

	}

}
