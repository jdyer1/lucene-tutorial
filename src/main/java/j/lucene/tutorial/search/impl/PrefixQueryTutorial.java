package j.lucene.tutorial.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PrefixQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;
import j.lucene.tutorial.search.impl.PrefixQueryTutorial.PrefixInput;

public class PrefixQueryTutorial extends QueryTutorialBase implements SearchTutorial<PrefixInput> {

	protected PrefixQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	@Override
	public SearchResults query(PrefixInput queryFor, int maxResults) {
		return executeSearch(new PrefixQuery(new Term(queryFor.field, queryFor.prefix)), maxResults);
	}

	public static class PrefixInput {
		private final String field;
		private final String prefix;

		public PrefixInput(String field, String prefix) {
			this.field = field;
			this.prefix = prefix;
		}

		public String getField() {
			return field;
		}

		public String getPrefix() {
			return prefix;
		}

	}

}
