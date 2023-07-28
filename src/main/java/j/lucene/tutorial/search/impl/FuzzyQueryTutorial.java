package j.lucene.tutorial.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;
import j.lucene.tutorial.search.impl.FuzzyQueryTutorial.FuzzyText;

/**
 * Demonstrates searching for text that is up to 2 edits away from the input.
 * This is an easy way to allow minor misspellings to match but also may
 * introduce false-positive matches.
 */
public class FuzzyQueryTutorial extends QueryTutorialBase implements SearchTutorial<FuzzyText> {

	protected FuzzyQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	/**
	 * There are mutliple tuning parameters on Lucene's Fuzzy Query. For instance,
	 * it is possible to change the maximum edit distance away from the default of
	 * 2. Here we use all the defaults.
	 */
	@Override
	public SearchResults query(FuzzyText queryFor, int maxResults) {
		return executeSearch(new FuzzyQuery(new Term(queryFor.field, queryFor.text)), maxResults);
	}

	public static class FuzzyText {
		private final String field;
		private final String text;

		public FuzzyText(String field, String text) {
			this.field = field;
			this.text = text;
		}

		public String getField() {
			return field;
		}

		public String getText() {
			return text;
		}

	}

}
