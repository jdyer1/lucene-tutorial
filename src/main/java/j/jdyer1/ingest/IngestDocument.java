package j.jdyer1.ingest;

import java.util.Collections;
import java.util.Map;

public class IngestDocument {
	private final Map<String, Object> contentByFieldname;

	public IngestDocument(Map<String, Object> contentByFieldname) {
		this.contentByFieldname = Collections.unmodifiableMap(contentByFieldname);
	}

	public Map<String, Object> getContentByFieldname() {
		return contentByFieldname;
	}

}
