package jp.ksgwr.lucene.kvs;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

public class LuceneKVSTest {

	@Test
	public void simpleTest() throws IOException {
		Map<String, Double> kvs = new LuceneKVS<String, Double>() {
			@Override
			public Double valueOf(String val) {
				return Double.valueOf(val);
			}
		};

		Double val = 0.1;

		kvs.put("test", val);

		Double valObj = kvs.get("test");

		assertEquals(val, valObj);
	}

}
