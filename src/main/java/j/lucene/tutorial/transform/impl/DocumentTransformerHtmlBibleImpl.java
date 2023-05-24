package j.lucene.tutorial.transform.impl;

import java.io.Reader;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import j.lucene.tutorial.extract.ExtractedDocument;
import j.lucene.tutorial.transform.DocumentTransformer;
import j.lucene.tutorial.transform.TransformedDocument;

/**
 * This Document Transformer takes an HTML BIble Chapter and transforms it to
 * Lucene Terms.
 *
 */
public class DocumentTransformerHtmlBibleImpl implements DocumentTransformer {

	@Override
	public TransformedDocument transformExtractedDocument(Analyzer a, ExtractedDocument in) {
		List<Field> luceneFields = new ArrayList<>(in.getContentByFieldname().size());
		for (Map.Entry<String, Object> entry : in.getContentByFieldname().entrySet()) {
			if (entry.getKey().equals("text")) {
				Reader r = new HTMLStripCharFilter(new StringReader(entry.getValue().toString()));
				luceneFields.add(new TextField(entry.getKey(), a.tokenStream(entry.getKey(), r)));
			} else if (entry.getValue() instanceof ZonedDateTime zdt) {
				long epochMilli = zdt.toInstant().toEpochMilli();
				luceneFields.add(new LongField(entry.getKey(), epochMilli, Store.NO));
			} else if (entry.getValue() instanceof String[] strArr) {
				for (String str : strArr) {
					luceneFields.add(new KeywordField(entry.getKey(), str, Store.YES));
				}
			} else {
				String str = entry.getValue().toString();
				luceneFields.add(new StringField(entry.getKey(), str, Store.NO));
			}
		}
		return new TransformedDocument(luceneFields);
	}

}
