package j.lucene.tutorial.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;
import j.lucene.tutorial.search.impl.WildcardQueryTutorial.WildcardInput;

public class WildcardQueryTutorial extends QueryTutorialBase implements SearchTutorial<WildcardInput> {

	protected WildcardQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	@Override
	public SearchResults query(WildcardInput queryFor, int maxResults) {
		return executeSearch(new WildcardQuery(new Term(queryFor.field, queryFor.value)), maxResults);
	}

	public static class WildcardInput {
		private final String field;
		private final String value;

		public WildcardInput(String field, String value) {
			this.field = field;
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public String getValue() {
			return value;
		}

	}
}
