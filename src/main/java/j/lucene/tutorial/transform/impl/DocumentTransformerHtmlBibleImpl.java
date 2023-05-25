package j.lucene.tutorial.transform.impl;

import java.io.Reader;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import j.lucene.tutorial.LuceneTutorialException;
import j.lucene.tutorial.extract.ExtractedDocument;
import j.lucene.tutorial.transform.DocumentTransformer;
import j.lucene.tutorial.transform.TransformedDocument;

/**
 * This Document Transformer takes an HTML Bible Chapter and transforms it to
 * Lucene Fields.
 * 
 * TODO: DocValues, Stored Text Fields
 *
 */
public class DocumentTransformerHtmlBibleImpl implements DocumentTransformer, AutoCloseable {

	private final Map<String, FromSomethingFieldProducer> fieldMapping;

	private boolean closed = false;

	public DocumentTransformerHtmlBibleImpl() {
		List<FromSomethingFieldProducer> fl = new ArrayList<>();
		fl.add(new FromIntegerIntProducer("chapter"));
		fl.add(new FromZonedDateTimeLongProducer("add_timestamp"));
		fl.add(new FromObjectStringFieldProducer("book"));
		fl.add(new FromObjectStringFieldProducer("source"));
		fl.add(new FromPlainTextFieldProvider("synopsis"));
		fl.add(new FromHtmlTextFieldProducer("text"));
		fl.add(new FromStringArrayKeywordsProducer("keywords"));
		this.fieldMapping = fl.stream()
				.collect(Collectors.toUnmodifiableMap(FromSomethingFieldProducer::name, Function.identity()));
	}

	@Override
	public TransformedDocument transformExtractedDocument(ExtractedDocument in) {
		if (closed) {
			throw new LuceneTutorialException("Cannot use this once it has been closed.");
		}
		List<Field> luceneFields = new ArrayList<>(in.getContentByFieldname().size());
		for (Map.Entry<String, Object> entry : in.getContentByFieldname().entrySet()) {
			FromSomethingFieldProducer producer = fieldMapping.get(entry.getKey());
			if (producer != null) {
				producer.addFields(entry.getValue(), luceneFields);
			}
		}
		return new TransformedDocument(luceneFields);
	}

	@Override
	public void close() throws Exception {
		closed = true;

		for (FromSomethingFieldProducer p : fieldMapping.values()) {
			if (p instanceof AutoCloseable ac) {
				ac.close();
			}
		}
	}

	private class FromHtmlTextFieldProducer extends FromSomethingFieldProducer implements AutoCloseable {
		final Analyzer a;

		FromHtmlTextFieldProducer(String name) {
			super(name);
			this.a = new StandardAnalyzer();
		}

		@Override
		void addFields(Object strVal, List<Field> luceneFields) {
			Reader r = new HTMLStripCharFilter(new StringReader(((String) strVal)));
			luceneFields.add(new TextField(name, a.tokenStream(name, r)));
		}

		@Override
		public void close() throws Exception {
			a.close();
		}
	}

	private class FromPlainTextFieldProvider extends FromSomethingFieldProducer implements AutoCloseable {
		final Analyzer a;

		FromPlainTextFieldProvider(String name) {
			super(name);
			this.a = new StandardAnalyzer();
		}

		@Override
		void addFields(Object strVal, List<Field> luceneFields) {
			luceneFields.add(new TextField(name, a.tokenStream(name, ((String) strVal))));
		}

		@Override
		public void close() throws Exception {
			a.close();
		}

	}

	private class FromZonedDateTimeLongProducer extends FromSomethingFieldProducer {

		FromZonedDateTimeLongProducer(String name) {
			super(name);
		}

		@Override
		void addFields(Object zdtVal, List<Field> luceneFields) {
			long epochMilli = ((ZonedDateTime) zdtVal).toInstant().toEpochMilli();
			luceneFields.add(new LongField(name, epochMilli, Store.NO));
		}

	}

	private class FromIntegerIntProducer extends FromSomethingFieldProducer {

		FromIntegerIntProducer(String name) {
			super(name);
		}

		@Override
		void addFields(Object integerVal, List<Field> luceneFields) {
			luceneFields.add(new IntField(name, (Integer) integerVal, Store.NO));
		}

	}

	private class FromStringArrayKeywordsProducer extends FromSomethingFieldProducer {

		FromStringArrayKeywordsProducer(String name) {
			super(name);
		}

		@Override
		void addFields(Object strArr, List<Field> luceneFields) {
			for (String str : ((String[]) strArr)) {
				luceneFields.add(new KeywordField(name, str, Store.YES));
			}
		}
	}

	private class FromObjectStringFieldProducer extends FromSomethingFieldProducer {

		FromObjectStringFieldProducer(String name) {
			super(name);
		}

		@Override
		void addFields(Object o, List<Field> luceneFields) {
			luceneFields.add(new StringField(name, o.toString(), Store.NO));
		}
	}

	private abstract class FromSomethingFieldProducer {
		final String name;

		FromSomethingFieldProducer(String name) {
			this.name = name;
		}

		String name() {
			return name;
		}

		abstract void addFields(Object data, List<Field> luceneFields);
	}

}
