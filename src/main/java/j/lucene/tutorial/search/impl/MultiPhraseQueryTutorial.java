package j.lucene.tutorial.search.impl;

import org.apache.lucene.search.MultiPhraseQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;

/**
 * Demonstrates the use of a phrase query with synonyms, that is, some or all of
 * the words in the phrase also match alternatives.
 */
public class MultiPhraseQueryTutorial extends QueryTutorialBase implements SearchTutorial<MultiPhraseQuery.Builder> {

	protected MultiPhraseQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	@Override
	public SearchResults query(MultiPhraseQuery.Builder queryFor, int maxResults) {
		return executeSearch(queryFor.build(), maxResults);
	}

}
