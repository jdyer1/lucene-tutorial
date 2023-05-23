package j.lucene.tutorial.extract;

import java.util.Collections;
import java.util.Map;

/**
 * Contains the raw data from a System Of Record extracted as a document that
 * can be added to a Lucene index.
 *
 */
public class ExtractedDocument {

	private final Map<String, Object> contentByFieldname;

	public ExtractedDocument(Map<String, Object> contentByFieldname) {
		this.contentByFieldname = Collections.unmodifiableMap(contentByFieldname);
	}

	public Map<String, Object> getContentByFieldname() {
		return contentByFieldname;
	}

}
