package j.lucene.tutorial.search.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import j.lucene.tutorial.load.impl.LuceneLoadingCollectorImpl;

class ScoringTutorialTest {

	private LuceneLoadingCollectorImpl collector;
	private Path tempDir;
	private DirectoryReader dr;

	// no annotation, each test case must call this,
	// because Similarity needs to be specified before indexing, and also when
	// searching.
	void setup(IndexWriterConfig iwc) throws Exception {
		this.collector = SearchTutorialTestBase.setup(iwc);
		this.tempDir = collector.localDiskLocation().getLocationPath();
		Directory dir = FSDirectory.open(tempDir);
		this.dr = DirectoryReader.open(dir);
	}

	@AfterEach
	void afterEach() throws Exception {
		this.dr.close();
		this.dr = null;
		SearchTutorialTestBase.teardown(collector);
		collector = null;
		tempDir = null;
	}

	@Test
	void testBm25() throws Exception {
		setup(null);
		List<ScoringTutorialResult> l = test(null);

		assertEquals("Isaiah 12", l.get(0).bookChapter,
				"The 1st hit has 4 occurrences of 'salvation' in a fairly short chapter.");

		assertEquals("Psalms 62", l.get(1).bookChapter,
				"The 2nd hit has 5 occurrences of 'salvation' in a somewhat longer chapter.");

		assertTrue("Habakkuk 3".equals(l.get(2).bookChapter) && "Psalms 85".equals(l.get(3).bookChapter),
				"BM25 should rank Hb3 higher than Ps85; Hb3 is a shorter document with more occurrences.");

		assertTrue(l.get(2).numTermsInDocument < l.get(3).numTermsInDocument,
				"The 2nd result should be longer than the 3rd result.");

		assertEquals(5, l.get(2).numOccurrencesTerm, "The 3rd result should have 5 occurrences of 'salvation'.");

		assertEquals(4, l.get(3).numOccurrencesTerm, "The 4th result should have 4 occurrences of 'salvation'.");

	}

	@Test
	void testClassicTfIdf() throws Exception {
		IndexWriterConfig iwc = new IndexWriterConfig();
		iwc.setSimilarity(new ClassicSimilarity());
		setup(iwc);
		List<ScoringTutorialResult> l = test(iwc.getSimilarity());

		assertEquals("Isaiah 12", l.get(0).bookChapter,
				"The 1st hit has 4 occurrences of 'salvation' in a fairly short chapter.");

		assertEquals("Psalms 62", l.get(1).bookChapter,
				"The 2nd hit has 5 occurrences of 'salvation' in a somewhat longer chapter.");

		assertTrue("Psalms 85".equals(l.get(2).bookChapter) && "Habakkuk 3".equals(l.get(3).bookChapter),
				"Compared to the Default BM25 model, VSM is ranking Ps85 higher than Hb3, putting the document that is both shorter with fewer occurrences after one longer with more occurrences.");

		assertTrue("Psalms 85".equals(l.get(2).bookChapter) && "Habakkuk 3".equals(l.get(3).bookChapter),
				"Classic VSM should rank PS85 higher than Hb3; PS85 is a longer document with fewer occurrences.");

		assertTrue(l.get(2).numTermsInDocument > l.get(3).numTermsInDocument,
				"The 2nd result should be shorter than the 3rd result.");

		assertEquals(4, l.get(2).numOccurrencesTerm, "The 3rd result should have 4 occurrences of 'salvation'.");

		assertEquals(5, l.get(3).numOccurrencesTerm, "The 4th result should have 5 occurrences of 'salvation'.");
	}

	@Test
	void testBm25NoLengthNorm() throws Exception {
		IndexWriterConfig iwc = new IndexWriterConfig();
		iwc.setSimilarity(new BM25Similarity(1.2f, .01f, true));
		setup(iwc);
		List<ScoringTutorialResult> l = test(iwc.getSimilarity());
		int lastNumOccur = Integer.MAX_VALUE;
		for (ScoringTutorialResult str : l) {
			assertTrue(str.numTermsInDocument <= lastNumOccur,
					"With no document length normalization, the term occurrences should descend.");
		}
	}

	private List<ScoringTutorialResult> test(Similarity sim) throws Exception {
		IndexSearcher is = new IndexSearcher(dr);
		if (sim != null) {
			is.setSimilarity(sim);
		}
		Term salvationTerm = new Term("text", "salvation");
		TermQuery tq = new TermQuery(salvationTerm);
		TopDocs td = is.search(tq, 10);

		List<ScoringTutorialResult> l = new ArrayList<>();
		float lastScore = Float.MAX_VALUE;
		for (ScoreDoc sd : td.scoreDocs) {

			ScoringTutorialResult stResult = new ScoringTutorialResult();
			stResult.bookChapter = bookChapter(is, sd);
			stResult.numTermsInDocument = documentLength(is, sd, "text");
			stResult.numOccurrencesTerm = numOccursInDocument(is, sd, salvationTerm);
			stResult.score = sd.score;
			l.add(stResult);

			assertTrue(sd.score <= lastScore, "Scores should descend.");
			lastScore = sd.score;
		}
		return l;
	}

	class ScoringTutorialResult {
		String bookChapter;
		float score;
		int numTermsInDocument;
		int numOccurrencesTerm;
	}

	private int documentLength(IndexSearcher is, ScoreDoc sd, String field) throws IOException {
		Document d = is.storedFields().document(sd.doc, Set.of(field));
		return d.get(field).split("\\s+").length;
	}

	private int numOccursInDocument(IndexSearcher is, ScoreDoc sd, Term t) throws IOException {
		Document d = is.storedFields().document(sd.doc, Set.of(t.field()));
		return (int) Arrays.stream(d.get(t.field()).split("\\s+")).map(s1 -> s1.replaceAll("[^a-zA-Z0-9]", ""))
				.filter(s -> s.toLowerCase(Locale.ROOT).equals(t.text())).count();
	}

	private String bookChapter(IndexSearcher is, ScoreDoc sd) throws IOException {
		Document d = is.storedFields().document(sd.doc);
		return d.get("book") + " " + d.get("chapter");
	}

}
