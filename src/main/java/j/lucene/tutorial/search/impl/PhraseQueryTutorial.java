package j.lucene.tutorial.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;
import j.lucene.tutorial.search.impl.PhraseQueryTutorial.PhraseQueryTutorialInput;

/**
 * Demonstrates how to match on phrases against text fields that are analyzed
 * and tokenized.
 * 
 */
public class PhraseQueryTutorial extends QueryTutorialBase implements SearchTutorial<PhraseQueryTutorialInput> {

	protected PhraseQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	/**
	 * search for a phrase.
	 */
	@Override
	public SearchResults query(PhraseQueryTutorialInput queryFor, int maxResults) {
		PhraseQuery.Builder b = new PhraseQuery.Builder().setSlop(queryFor.editDistance);
		for (String phraseWord : queryFor.phraseWords) {
			b.add(new Term(queryFor.field, phraseWord));
		}
		return executeSearch(b.build(), maxResults);
	}

	public static class PhraseQueryTutorialInput {
		private final String field;
		private final String[] phraseWords;
		private final int editDistance;

		/**
		 * The words must be ajacent in the document in order to match.
		 * 
		 * @param field
		 * @param phraseWords
		 */
		public PhraseQueryTutorialInput(String field, String... phraseWords) {
			this.field = field;
			this.editDistance = 0;
			this.phraseWords = phraseWords;
		}

		/**
		 * The "editDistance" (aka: slop) lets you specify how far apart the words can
		 * be in order to match.
		 * 
		 * @param field
		 * @param editDistance
		 * @param phraseWords
		 */
		public PhraseQueryTutorialInput(String field, int editDistance, String... phraseWords) {
			this.field = field;
			this.editDistance = editDistance;
			this.phraseWords = phraseWords;
		}
	}

}
