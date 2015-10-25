package jp.ksgwr.lucene.valuesdb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jp.ksgwr.lucene.valuesdb.annotation.NoIndex;
import jp.ksgwr.lucene.valuesdb.annotation.TextFieldable;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;

/**
 * Lucene Object Values DB
 *
 * TODO: map,list,arrayがpremitiveなオブジェクトしかサポートしてない。
 *
 * @author ksgwr
 *
 * @param <T> target object
 */
public class LuceneObjectValuesDB<T> extends LuceneValuesDB {

	/** target class */
	protected Class<T> target;

	/** is multiple values in a field name */
	protected Boolean isMultiples[];

	/** if true , create empty instance when not found field */
	protected boolean isInitialzieEmptyMultiplesField;

	/** if false, set null plain object when any field not found */
	protected boolean isInitializeEmptyObject;

	/**
	 * constructor
	 * @param directory index directory
	 * @param indexFile index file path
	 * @param isVolatile if true, delete index on exit
	 * @param analyzer TextField analyzer
	 * @param target target class
	 * @throws IOException something error
	 */
	public LuceneObjectValuesDB(Directory directory, File indexFile, boolean isVolatile, Analyzer analyzer, Class<T> target) throws IOException {
		super(directory, indexFile, isVolatile, analyzer);
		initFieldTypes(target);
	}

	/**
	 * constructor
	 * @param directory index directory
	 * @param indexFile indef file path
	 * @param isVolatile if true, delete index on exit
	 * @param target target class
	 * @throws IOException something error
	 */
	public LuceneObjectValuesDB(Directory directory, File indexFile, boolean isVolatile, Class<T> target) throws IOException {
		super(directory, indexFile, isVolatile);
		initFieldTypes(target);
	}

	/**
	 * constructor
	 * @param target target class
	 * @throws IOException something error
	 */
	public LuceneObjectValuesDB(Class<T> target) throws IOException {
		super();
		initFieldTypes(target);
	}

	/**
	 * set isInitialzieEmptyMultiplesField
	 * @param isInitialzieEmptyMultiplesField if true , create empty instance when not found field
	 */
	public void setInitializeEmptyMultiplesField(boolean isInitialzieEmptyMultiplesField) {
		this.isInitialzieEmptyMultiplesField = isInitialzieEmptyMultiplesField;
	}

	/**
	 * isInitialzieEmptyMultiplesField
	 * @return if true , create empty instance when not found field
	 */
	public boolean isInitializeEmptyMultiplesField() {
		return isInitialzieEmptyMultiplesField;
	}

	/**
	 * set isInitializeEmptyObject
	 * @param isInitializeEmptyObject if false, set null plain object when any field not found
	 */
	public void setInitializeEmptyObject(boolean isInitializeEmptyObject) {
		this.isInitializeEmptyObject = isInitializeEmptyObject;
	}

	/**
	 * isInitializeEmptyObject
	 * @return if false, set null plain object when any field not found
	 */
	public boolean isInitializeEmptyObject() {
		return isInitializeEmptyObject;
	}

	/**
	 * initialzie field list
	 * @param target target class
	 */
	@SuppressWarnings("unchecked")
	protected void initFieldTypes(Class<T> target) {
		this.target = target;

		List<String> fieldNames = new ArrayList<String>();
		List<Class<? extends Field>> fieldTypes = new ArrayList<Class<? extends Field>>();
		List<Store> stores = new ArrayList<Store>();
		List<Boolean> isMultiples = new ArrayList<Boolean>();

		initList("", target, fieldNames, fieldTypes, stores, isMultiples, null);

		this.fieldNames = fieldNames.toArray(new String[0]);
		this.fieldTypes = fieldTypes.toArray(new Class[0]);
		this.stores = stores.toArray(new Store[0]);
		this.isMultiples = isMultiples.toArray(new Boolean[0]);

		this.fieldTypeEnums = new FieldEnum[fieldTypes.size()];
		for(int i=0;i<fieldTypes.size();i++) {
			this.fieldTypeEnums[i] = FieldEnum.valueOf(fieldTypes.get(i).getSimpleName());
		}
		this.fieldsIndex = new HashMap<String, Integer>();
		for(int i=0;i<fieldNames.size();i++) {
			this.fieldsIndex.put(fieldNames.get(i), i);
		}
	}

	/**
	 * initialize field list
	 * @param parent parent path
	 * @param target target class
	 * @param fieldNames field name
	 * @param fieldTypes field type
	 * @param stores store
	 * @param isMultiples is multiple
	 * @param noIndex noIndex annotation
	 */
	private void initList(String parent, Class<?> target, List<String> fieldNames, List<Class<? extends Field>> fieldTypes, List<Store> stores, List<Boolean> isMultiples, NoIndex noIndex) {
		for(java.lang.reflect.Field field:target.getDeclaredFields()) {
			int mod = field.getModifiers();
			if(Modifier.isTransient(mod) || Modifier.isStatic(mod) || !Modifier.isPublic(mod)) {
				continue;
			}
			if(noIndex!=null) {
				noIndex = field.getAnnotation(NoIndex.class);
			}
			TextFieldable textFieldable = field.getAnnotation(TextFieldable.class);
			boolean isMultiple = false;
			Class<? extends Field> fieldType = null;
			if (noIndex!=null) {
				fieldType = StoredField.class;
			} else {
				Type type = field.getType();
				if (((Class<?>) type).isArray()) {
					// if Array, overwrite inner field type
					type = ((Class<?>)type).getComponentType();
					// convert char[] => String, byte[] => StoredField
					if(type == char.class) {
						fieldType = StringField.class;
					} else if(type == byte.class) {
						fieldType = StoredField.class;
					} else {
						isMultiple = true;
					}
				}
				if (fieldType == null) {
					fieldType = resolveField(type);
				}
				if (fieldType == null) {
					Class<?> clazz = field.getType();
					if (Collections.class.isAssignableFrom(clazz)
							|| List.class.isAssignableFrom(clazz)
							|| Set.class.isAssignableFrom(clazz)) {
						ParameterizedType pt = (ParameterizedType) field.getGenericType();
						isMultiple = true;
						fieldType = resolveField(pt.getActualTypeArguments()[0]);
					} else if (Map.class.isAssignableFrom(clazz)) {
						ParameterizedType pt = (ParameterizedType) field.getGenericType();
						Type[] types = pt.getActualTypeArguments();
						isMultiple = true;
						fieldType = resolveField(types[0]);
						if (fieldType == null) {
							throw new UnsupportedOperationException(
									"MapObject " + field.getName()
											+ " not supported ungeneral types");
						}
						if(textFieldable != null && textFieldable.key()) {
							fieldType = TextField.class;
						}
						isMultiples.add(isMultiple);
						fieldTypes.add(fieldType);
						fieldNames.add(parent + field.getName() + ".key");
						stores.add(Store.YES);

						fieldType = resolveField(types[1]);
						if (fieldType == null) {
							throw new UnsupportedOperationException(
									"MapObject " + field.getName()
											+ " not supported ungeneral types");
						}
						if(textFieldable != null && textFieldable.value()) {
							fieldType = TextField.class;
						}
						isMultiples.add(isMultiple);
						fieldTypes.add(fieldType);
						fieldNames.add(parent + field.getName() + ".val");
						stores.add(Store.YES);
						continue;
					} else {
						// plain object
						initList(parent + field.getName() + ".", field.getType(), fieldNames,
								fieldTypes, stores, isMultiples, noIndex);
						continue;
					}
				}
			}
			if(noIndex != null && textFieldable != null) {
				fieldType = TextField.class;
			}
			isMultiples.add(isMultiple);
			fieldTypes.add(fieldType);
			fieldNames.add(parent + field.getName());
			stores.add(Store.YES);

		}
	}

	/**
	 * resolve field convertable premitive type
	 *
	 * premitive type
	 * byte, short, int, long, float, double, char, boolean
	 *
	 * @param type field type
	 * @return lucene field type
	 */
	private Class<? extends Field> resolveField(Type type) {
		if(type == String.class) {
			return StringField.class;
		} else if (type == Double.class || type == double.class) {
			return DoubleField.class;
		} else if(type == Float.class || type == float.class) {
			return FloatField.class;
		} else if(type == Integer.class || type == int.class ||
				type == Short.class || type == short.class ||
				type == Boolean.class || type == boolean.class ||
				type == Byte.class || type == byte.class ||
				type == Character.class || type == char.class) {
			return IntField.class;
		} else if(type == Long.class || type == long.class) {
			return LongField.class;
		}
		return null;
	}

	/**
	 * add Object
	 * @param obj Object
	 * @throws IOException something error
	 */
	public void addObject(T obj) throws IOException {
		Document doc = new Document();
		try {
			addObject(obj, doc, "", null);
			writer.addDocument(doc);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * add Object as lucene document
	 * @param obj object
	 * @param doc document
	 * @param parent parent path
	 * @param noIndex noIndex annotation
	 * @throws IllegalArgumentException something error
	 * @throws IllegalAccessException something error
	 */
	private void addObject(Object obj, Document doc, String parent,NoIndex noIndex) throws IllegalArgumentException, IllegalAccessException {
		for(java.lang.reflect.Field field:obj.getClass().getDeclaredFields()) {
			int mod = field.getModifiers();
			if (noIndex!=null) {
				// noIndex propagate child object
				noIndex = field.getAnnotation(NoIndex.class);
			}
			if(Modifier.isTransient(mod) || Modifier.isStatic(mod) || !Modifier.isPublic(mod)) {
				continue;
			}
			TextFieldable textFieldable = field.getAnnotation(TextFieldable.class);

			Object val = field.get(obj);
			if (val==null) {
				continue;
			}

			String name = parent + field.getName();
			Integer index = fieldsIndex.get(name);
			Type type = field.getType();
			Class<?> clazz = field.getType();
			if (index==null) {
				// not found field name, map field or plain object
				if (Map.class.isAssignableFrom(clazz)) {
					index = fieldsIndex.get(name+".key");
					ParameterizedType pt = (ParameterizedType) field.getGenericType();
					Type[] types = pt.getActualTypeArguments();
					@SuppressWarnings("rawtypes")
					Map<?,?> map = (Map)val;
					TextFieldable keyTextFieldable = null;
					TextFieldable valTextFieldable = null;
					if (textFieldable != null) {
						if (textFieldable.key()) {
							keyTextFieldable = textFieldable;
						}
						if (textFieldable.value()) {
							valTextFieldable = textFieldable;
						}
					}
					for(Entry<?,?> entry : map.entrySet()) {
						val = entry.getKey();
						addField(doc, name+".key", val, types[0], stores[index], keyTextFieldable, noIndex);
						val = entry.getValue();
						addField(doc, name+".val", val, types[1], stores[index+1], valTextFieldable, noIndex);
					}
					index = index + 2;
				} else {
					// plain object
					addObject(val, doc, name+".", noIndex);
				}
				continue;
			}

			Store store = stores[index];
			if(clazz.isArray()) {
				type = clazz.getComponentType();
				if(type == char.class) {
					addField(doc, name, new String((char[])val), String.class, store, textFieldable, noIndex);
				} else if(type == byte.class) {
					doc.add(new StoredField(name, (byte[])val));
				} else {
					Object array = val;
					int length = Array.getLength(array);
					for (int i = 0; i < length; i++) {
						val = Array.get(array, i);
						addField(doc, name, val, type, store, textFieldable, noIndex);
					}
				}
			} else {
				// if not single field, detect interface
				if (!addField(doc, name, val, type, store, textFieldable, noIndex)) {
					if (Collections.class.isAssignableFrom(clazz)
							|| List.class.isAssignableFrom(clazz)
							|| Set.class.isAssignableFrom(clazz)) {
						ParameterizedType pt = (ParameterizedType) field.getGenericType();
						type = pt.getActualTypeArguments()[0];
						@SuppressWarnings("rawtypes")
						Collection coll = (Collection)val;
						for(Object colval : coll) {
							addField(doc, name, colval, type, store, textFieldable, noIndex);
						}
					}
				}
			}
		}
	}

	/**
	 * add lucene field in document
	 * @param doc document
	 * @param name fieldName
	 * @param val value
	 * @param type field original Type
	 * @param store store
	 * @param textFieldable isTextField
	 * @return if true, added document
	 */
	private boolean addField(Document doc, String name, Object val, Type type, Store store, TextFieldable textFieldable, NoIndex noIndex) {
		boolean add = true;
		if (noIndex != null) {
			if (type == Character.class || type == char.class) {
				val = (int) val;
			} else if(type == Boolean.class || type == boolean.class) {
				val = (boolean)val ? 1 : 0;
			}
			doc.add(new StoredField(name, val.toString()));
		} else if (textFieldable != null) {
			doc.add(new TextField(name, val.toString(), store));
		} else if(type == String.class) {
			doc.add(new StringField(name, val.toString(), store));
		}else if (type == Double.class || type == double.class) {
			doc.add(new DoubleField(name, (double) val, store));
		} else if(type == Float.class || type == float.class) {
			doc.add(new FloatField(name, (float) val, store));
		} else if(type == Short.class || type == short.class ||
				type == Integer.class || type == int.class ||
				type == Byte.class || type == byte.class) {
			doc.add(new IntField(name, Integer.valueOf(val.toString()), store));
		} else if(type == Character.class || type == char.class) {
			doc.add(new IntField(name, Integer.valueOf((char)val), store));
		} else if(type == Boolean.class || type == boolean.class) {
			if ((boolean)val) {
				doc.add(new IntField(name, 1, store));
			} else {
				doc.add(new IntField(name, 0, store));
			}
		} else if(type == Long.class || type == long.class) {
			doc.add(new LongField(name, (long) val, store));
		} else {
			add = false;
		}
		return add;
	}

	/**
	 * commit documents
	 * @throws IOException something error
	 */
	public void commit() throws IOException {
		writer.commit();
		manager.maybeRefreshBlocking();
	}

	@Override
	public List<String> convertFields(Document document) {
		ArrayList<String> fields = new ArrayList<String>(fieldNames.length);
		for(int i=0;i<fieldNames.length;i++) {
			if (isMultiples[i]) {
				for(String val:document.getValues(fieldNames[i])) {
					fields.add(val);
				}
			} else {
				fields.add(document.get(fieldNames[i]));
			}

		}
		return fields;
	}

	/**
	 * createDataInstance
	 * @return newInstance
	 */
	protected T createDataInstance() {
		try {
			return this.target.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * convert object list
	 * @param documents documents list
	 * @return list object
	 */
	public List<T> convertObjectList(Document[] documents) {
		List<T> result = new ArrayList<T>(documents.length);
		for(Document doc:documents) {
			result.add(convertObject(doc));
		}
		return result;
	}

	/**
	 * convert object
	 * @param document lucene document
	 * @return object
	 */
	public T convertObject(Document document) {
		T obj = null;
		try {
			obj = this.createDataInstance();
			convertObject(document, obj, "", 0);
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
		}

		return obj;
	}

	/**
	 * convert Object
	 * @param document document root
	 * @param obj object
	 * @param parent parent path
	 * @param index index num
	 * @return index num
	 * @throws NoSuchFieldException something error
	 * @throws SecurityException something error
	 * @throws IllegalArgumentException something error
	 * @throws IllegalAccessException something error
	 * @throws InstantiationException something error
	 */
	@SuppressWarnings("unchecked")
	private int convertObject(Document document, Object obj, String parent, int index) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException {
		java.lang.reflect.Field field;
		for(;index<fieldNames.length;index++) {
			String fieldName = fieldNames[index];
			if (fieldName.startsWith(parent)) {
				fieldName = fieldName.substring(parent.length());
				int c;
				if ((c = fieldName.indexOf("."))>0) {
					String subParent = fieldName.substring(0, c);
					if ("key".equals(fieldName.substring(c+1))) {
						// map
						field = obj.getClass().getField(subParent);
						@SuppressWarnings("rawtypes")
						Map map = null;
						String[] keys = document.getValues(fieldNames[index++]);
						String[] vals = document.getValues(fieldNames[index]);
						if(keys.length!=0 || isInitialzieEmptyMultiplesField) {
							map = createMap(field.getType());
							ParameterizedType pt = (ParameterizedType) field.getGenericType();
							Type[] types = pt.getActualTypeArguments();
							for(int i=0;i<keys.length;i++) {
								map.put(convertField(types[0], keys[i]), convertField(types[1], vals[i]));
							}
							field.set(obj, map);
						}
					} else {
						// plain object
						boolean requireConvert = true;
						if(!isInitializeEmptyObject) {
							requireConvert = false;
							// search child field in document
							for(IndexableField subField: document.getFields()) {
								if (subField.name().indexOf(
										parent + subParent + ".") >= 0) {
									requireConvert = true;
									break;
								}
							}
						}
						if (requireConvert) {
							field = obj.getClass().getField(subParent);
							Object child = field.getType().newInstance();
							index = convertObject(document, child, parent + subParent + ".", index);
							field.set(obj, child);
							index--;
						}
					}
				} else {
					// premitive convertable field
					field = obj.getClass().getField(fieldName);
					Class<?> type = field.getType();
					if (isMultiples[index]) {
						Class<?> clazz = field.getType();
						if (clazz.isArray()) {
							String[] vals = document.getValues(fieldNames[index]);
							if(vals.length!=0 || isInitialzieEmptyMultiplesField) {
								Class<?> componentType = clazz.getComponentType();
								Object val = Array.newInstance(componentType, vals.length);
								for (int i=0;i<vals.length;i++) {
									Array.set(val, i, convertField(componentType, vals[i]));
								}
								field.set(obj, val);
							}
						} else if(Collections.class.isAssignableFrom(clazz)
								|| List.class.isAssignableFrom(clazz)
								|| Set.class.isAssignableFrom(clazz)) {
							String[] vals = document.getValues(fieldNames[index]);
							if(vals.length!=0 || isInitialzieEmptyMultiplesField) {
								ParameterizedType pt = (ParameterizedType) field.getGenericType();
								Type[] types = pt.getActualTypeArguments();
								@SuppressWarnings("rawtypes")
								Collection coll = createCollection(clazz);
								for(String val:vals) {
									coll.add(convertField(types[0], val));
								}
								field.set(obj, coll);
							}


						}
						// document.getBinaryValues(fieldNames[i]);
					} else if(type == byte[].class){
						BytesRef bref = document.getBinaryValue(fieldNames[index]);
						if (bref!=null) {
							field.set(obj, bref.bytes);
						} else if(isInitialzieEmptyMultiplesField){
							field.set(obj, new byte[0]);
						} else {
							field.set(obj, null);
						}
					} else {
						String val = document.get(fieldNames[index]);
						if (val!=null) {
							field.set(obj, convertField(type, val));
						}
					}
				}
			} else {
				// not found field
				break;
			}
		}
		return index;
	}

	/**
	 * create Map instance
	 * you can override this method, if you need
	 * @param clazz extend map class
	 * @return collection instance
	 * @throws InstantiationException something error
	 * @throws IllegalAccessException something error
	 */
	@SuppressWarnings("rawtypes")
	protected Map createMap(Class<?> clazz) throws InstantiationException, IllegalAccessException {
		if (clazz.isInterface()) {
			return new HashMap();
		} else {
			// use default constructor
			return (Map) clazz.newInstance();
		}
	}

	/**
	 * create Collection instance
	 * you can override this method, if you need
	 * @param clazz extend collection class
	 * @return collection instance
	 * @throws InstantiationException something error
	 * @throws IllegalAccessException something error
	 */
	@SuppressWarnings("rawtypes")
	protected Collection createCollection(Class<?> clazz) throws InstantiationException, IllegalAccessException {
		if (clazz.isInterface()) {
			if (clazz == Set.class) {
				return new LinkedHashSet();
			} else {
				// default collection
				return new ArrayList();
			}
		} else {
			// use default constructor
			return (Collection) clazz.newInstance();
		}
	}

	/**
	 * convert value
	 * @param type field type
	 * @param val value
	 * @return converted value
	 */
	private Object convertField(Type type, String val) {
		if(type == String.class) {
			return val;
		} else if (type == Double.class || type == double.class) {
			return Double.valueOf(val);
		} else if(type == Float.class || type == float.class) {
			return Float.valueOf(val);
		} else if(type == Short.class || type == short.class) {
			return Short.valueOf(val);
		} else if(type == Integer.class || type == int.class) {
			return Integer.valueOf(val);
		} else if(type == Byte.class || type == byte.class) {
			return Byte.valueOf(val);
		} else if(type == Boolean.class || type == boolean.class) {
			return "1".equals(val) ? true : false;
		} else if(type == Long.class || type == long.class) {
			return Long.valueOf(val);
		} else if(type == Character.class || type == char.class) {
			return (char)(0 + Integer.valueOf(val.toString()));
		} else if(type == char[].class) {
			return val.toCharArray();
		}{
			return null;
		}
	}

	/**
	 * search Objects(OR)
	 * @param objs objects
	 * @param n max results
	 * @return hit document list
	 * @throws IOException something error
	 */
	public Document[] search(T[] objs, int n) throws IOException {
		return search(createQuery(objs), n);
	}

	/**
	 * search Objects(OR)
	 * @param objs objects
	 * @return hit document list
	 * @throws IOException something error
	 */
	public Document[] search(T[] objs) throws IOException {
		return search(createQuery(objs));
	}

	/**
	 * search Object
	 * @param obj object
	 * @param n max results
	 * @return hit document list
	 * @throws IOException something error
	 */
	public Document[] search(T obj, int n) throws IOException {
		return search(createQuery(obj), n);
	}

	/**
	 * search Object
	 * @param obj object
	 * @return hit document list
	 * @throws IOException something error
	 */
	public Document[] search(T obj) throws IOException {
		return search(createQuery(obj));
	}

	/**
	 * create Query
	 * @param objs objects
	 * @return query
	 */
	public Query createQuery(T[] objs) {
		try {
			BooleanQuery query = new BooleanQuery();
			for(T obj:objs) {
				 BooleanQuery subQuery = addQuery(new BooleanQuery(), obj, "", Occur.MUST);
				 query.add(subQuery, Occur.SHOULD);
			}
			return query;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * create Query
	 * @param obj object
	 * @return query
	 */
	public Query createQuery(T obj) {
		try {
			return addQuery(new BooleanQuery(), obj, "", Occur.MUST);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * add boolean query, scanning object
	 * @param query query
	 * @param obj object
	 * @param parent parent path
	 * @param occur occur(default must)
	 * @return boolean query
	 * @throws IllegalArgumentException something error
	 * @throws IllegalAccessException something error
	 */
	private BooleanQuery addQuery(BooleanQuery query, Object obj, String parent, Occur occur) throws IllegalArgumentException, IllegalAccessException {
		for(java.lang.reflect.Field field:target.getDeclaredFields()) {
			int mod = field.getModifiers();
			if(Modifier.isTransient(mod) || Modifier.isStatic(mod) || !Modifier.isPublic(mod)) {
				continue;
			}
			if(field.getAnnotation(NoIndex.class)!=null) {
				continue;
			}
			Object valObj = field.get(obj);
			if(valObj==null) {
				continue;
			}

			String fieldName = parent + field.getName();
			Integer fieldNum = fieldsIndex.get(fieldName);
			Class<?> fieldClass = field.getType();

			if (fieldNum==null) {
				if (Map.class.isAssignableFrom(fieldClass)) {
					fieldNum = fieldsIndex.get(fieldName+".key");
					ParameterizedType pt = (ParameterizedType) field.getGenericType();
					Type[] types = pt.getActualTypeArguments();
					@SuppressWarnings("rawtypes")
					Map<?, ?> map = (Map)valObj;
					Object[] keys = new Object[map.size()];
					Object[] vals = new Object[map.size()];
					int i=0;
					for(Entry<?,?> entry : map.entrySet()) {
						keys[i] = entry.getKey();
						vals[i] = entry.getValue();
						i++;
					}
					query = addQuery(query, keys, fieldName+".key", fieldTypeEnums[fieldNum], (Class<?>) types[0], occur);
					query = addQuery(query, vals, fieldName+".val", fieldTypeEnums[fieldNum+1], (Class<?>) types[1], occur);
				} else {
					// plain object
					query = addQuery(query, valObj, fieldName+".", occur);
				}
			} else {
				FieldEnum fieldType = fieldTypeEnums[fieldNum];
				Object[] vals;

				if (isMultiples[fieldNum]) {
					if (fieldClass.isArray()) {
						// Array
						vals = new Object[Array.getLength(valObj)];
						for(int i=0;i<vals.length;i++) {
							vals[i] = Array.get(valObj, i);
						}
						fieldClass = fieldClass.getComponentType();
					} else {
						// Collection
						ParameterizedType pt = (ParameterizedType) field.getGenericType();
						Type[] types = pt.getActualTypeArguments();
						fieldClass = (Class<?>) types[0];
						@SuppressWarnings("rawtypes")
						Collection collection = (Collection)valObj;
						vals = collection.toArray();
					}
				} else {
					if (fieldClass==char[].class) {
						valObj = new String((char[])valObj);
					}
					vals = new Object[]{valObj};
				}
				query = addQuery(query, vals, fieldName, fieldType, fieldClass, occur);
			}
		}
		return query;
	}

	/**
	 * add boolean query, detect field type
	 * @param query query
	 * @param vals values
	 * @param fieldName lucene fieldName
	 * @param fieldType lucene fieldType
	 * @param fieldClass fieldClass
	 * @param occur occur(default must)
	 * @return boolean query
	 */
	private BooleanQuery addQuery(BooleanQuery query, Object[] vals, String fieldName, FieldEnum fieldType, Class<?> fieldClass, Occur occur) {
		switch(fieldType) {
		case DoubleField:
			for(Object val:vals) {
				if (val==null) {
					continue;
				}
				double doubleValue = (double) val;
				query.add(NumericRangeQuery.newDoubleRange(fieldName, doubleValue, doubleValue, true, true), occur);
			}
			break;
		case FloatField:
			for(Object val:vals) {
				if (val==null) {
					continue;
				}
				float floatValue = (float) val;
				query.add(NumericRangeQuery.newFloatRange(fieldName, floatValue, floatValue, true, true), occur);
			}
			break;
		case IntField:
			// char or booleanなら特別処理が必要
			for(Object val:vals) {
				if (val==null) {
					continue;
				}
				int intValue;
				if (fieldClass == Boolean.class || fieldClass == boolean.class) {
					intValue = (boolean)val ? 1 : 0;
				} else if(fieldClass == Character.class || fieldClass == char.class) {
					intValue = (int)val;
				} else {
					intValue = Integer.valueOf(val.toString());
				}
				query.add(NumericRangeQuery.newIntRange(fieldName, intValue, intValue, true, true), occur);
			}
			break;
		case LongField:
			for(Object val:vals) {
				if (val==null) {
					continue;
				}
				long longValue = (long) val;
				query.add(NumericRangeQuery.newLongRange(fieldName, longValue, longValue, true, true), occur);
			}
			break;
		case StoredField:
			// not searchable
			break;
		case StringField:
			for(Object val:vals) {
				if (val==null) {
					continue;
				}
				query.add(new TermQuery(new Term(fieldName, val.toString())), occur);
			}
			break;
		case TextField:
			QueryParser parser = createQueryParser();
			for(Object val:vals) {
				if (val==null) {
					continue;
				}
				query.add(parser.createPhraseQuery(fieldName, val.toString()), occur);
			}
			break;
		}
		return query;
	}
}
