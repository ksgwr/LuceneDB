package jp.ksgwr.lucene.valuesdb;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jp.ksgwr.lucene.valuesdb.annotation.NoIndex;
import jp.ksgwr.lucene.valuesdb.annotation.TextFieldable;

import org.apache.lucene.document.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LuceneObjectValuesDBComplexTest {

	private LuceneObjectValuesDB<Sample> valuesDB;

	private Sample sample;

	private Sample empty;

	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Before
	public void setup() throws IOException {
		valuesDB = new LuceneObjectValuesDB<Sample>(Sample.class);

		sample = new Sample();
		sample.str1 = "str";
		sample.str2 = "str str";
		sample.int1 = 1;
		sample.int2 = 2;
		sample.double1 = 0.1;
		sample.double2 = 0.2;
		sample.float1 = 0.1f;
		sample.float2 = 0.2f;
		sample.short1 = 1;
		sample.short2 = 2;
		sample.bool1 = true;
		sample.bool2 = true;
		sample.long1 = 1L;
		sample.long2 = 2L;
		sample.byte1 = 1;
		sample.byte2 = 2;
		sample.char1 = 'a';
		sample.char2 = 'b';
		sample.chararray1 = new char[]{'a', 'b'};

		sample.child = new Child();
		sample.child.val = "child_val";

		sample.bytearray = new byte[]{1};
		sample.strarray = new String[] {"foo", "bar"};
		sample.strlist = new ArrayList<>();
		sample.strlist.add("piyo");
		sample.strlist2 = new LinkedList<>();
		sample.strlist2.add("puyo");
		sample.map = new HashMap<>();
		sample.map.put("map1", "val1");

		sample.bool3 = true;
		sample.char3 = 'c';
		sample.int3 = 3;

		sample.child2 = new Child();
		sample.child2.val = "child_val2";

		valuesDB.addObject(sample);

		empty = new Sample();
		empty.str1 = "str2";

		valuesDB.addObject(empty);

		valuesDB.commit();
	}

	@After
	public void cleanup() throws IOException {
		valuesDB.close();
	}

	@Test
	public void convertTest() throws IOException {
		Document[] docs = valuesDB.search("str1", "str");
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(sample, results.get(0));
	}

	@Test
	public void convertEmptyTest() throws IOException {
		Document[] docs = valuesDB.search("str1", "str2");
		List<Sample> results = valuesDB.convertObjectList(docs);

		assertEquals(1, results.size());
		assertEquals(empty, results.get(0));
	}

	/**
	 * sample pojo class
	 *
	 * @author ksgwr
	 *
	 */
	public static class Sample {
		public String str1;
		@TextFieldable
		public String str2;
		public int int1;
		public Integer int2;
		public double double1;
		public Double double2;
		public float float1;
		public Float float2;
		public short short1;
		public Short short2;
		public boolean bool1;
		public Boolean bool2;
		public long long1;
		public Long long2;
		public byte byte1;
		public Byte byte2;
		public char char1;
		public Character char2;
		public char[] chararray1;

		public Child child;

		public byte[] bytearray;
		public String[] strarray;
		public List<String> strlist;
		public LinkedList<String> strlist2;
		public Map<String,String> map;

		@NoIndex
		public Boolean bool3;
		@NoIndex
		public char char3;
		@NoIndex
		public int int3;

		public Long def = 123456L;

		public Child child2;

		@Override
		public boolean equals(Object obj) {
			return gson.toJson(this).equals(gson.toJson(obj));
		}

		public String toString() {
			return gson.toJson(this);
		}
	}

	public static class Child {
		public String val;
	}
}
