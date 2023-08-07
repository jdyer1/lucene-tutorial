package j.lucene.tutorial.search.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MultiPhraseQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import j.lucene.tutorial.extract.impl.DocumentExtractorBibleZipImpl;
import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.load.impl.LuceneLoadingCollectorImpl;
import j.lucene.tutorial.search.SearchResult;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.impl.FuzzyQueryTutorial.FuzzyText;
import j.lucene.tutorial.search.impl.IntegerRangeQueryTutorial.IntegerRange;
import j.lucene.tutorial.search.impl.PhraseQueryTutorial.PhraseQueryTutorialInput;
import j.lucene.tutorial.search.impl.PrefixQueryTutorial.PrefixInput;
import j.lucene.tutorial.search.impl.WildcardQueryTutorial.WildcardInput;
import j.lucene.tutorial.transform.impl.DocumentTransformerHtmlBibleImpl;

class QueryTutorialsTest {

	private static LuceneLoadingCollectorImpl loader;
	private static Path tempDir;

	private QueryTutorialBase currentTest = null;

	@BeforeAll
	static void beforeAll() throws Exception {
		tempDir = Files.createTempDirectory(QueryTutorialsTest.class.getSimpleName());
		loader = new LuceneLoadingCollectorImpl(new IndexPhysicalLocation(tempDir));
		loader.postConstruct();

		try (DocumentTransformerHtmlBibleImpl transformer = new DocumentTransformerHtmlBibleImpl()) {

			DocumentExtractorBibleZipImpl extractor = new DocumentExtractorBibleZipImpl();

			Path zipPath = Paths.get(".").toAbsolutePath().normalize().resolve("src").resolve("test")
					.resolve("resources").resolve("kj_new.zip");

			extractor.documentsFromFilePath(zipPath).map(transformer::transformExtractedDocument).collect(loader);
		}
	}

	@AfterEach
	void after() {
		if (currentTest != null) {
			currentTest.preDestroy();
		}
		currentTest = null;
	}

	@AfterAll
	static void afterAll() throws Exception {
		loader.preDestroy();
		Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

	@Test
	void testTermQuery() {
		TermQueryTutorial tqt = new TermQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = tqt;
		tqt.postConstruct();

		SearchResults matthew = tqt.query(new Term("book", "Matthew"), 5);
		assertEquals(28l, matthew.getTotalHits(),
				"There should be 28 documents returned, one per chapter in the book of Matthew.");
		assertFalse(matthew.isTotalApproximate(), "The total hits should be exact.");
		assertEquals(5l, matthew.getResults().size(), "Having requested 5 results, only 5 should be returned.");

		SearchResults matthewWrongCase = tqt.query(new Term("book", "matthew"), 5);
		assertEquals(0l, matthewWrongCase.getTotalHits(),
				"There should be no hits, because the query is case-sensitive.");

		SearchResults thirdJohn = tqt.query(new Term("book", "3 John"), 5);
		assertEquals(1l, thirdJohn.getTotalHits(), "There should be 1 hit because third john has 1 chapter.");
		assertEquals(1l, thirdJohn.getResults().size(),
				"There should be 1 result returned even though we requested up to 5.");

		SearchResult onlyChapterIn3j = thirdJohn.getResults().iterator().next();
		Map<String, Object> fieldData = onlyChapterIn3j.getValues();
		assertEquals(6, fieldData.size(),
				"There should be x fields as our code combined the Stored Fields with the Doc Values");

		Object keywordObj = fieldData.get("keywords");
		assertTrue(keywordObj instanceof List);
		@SuppressWarnings("unchecked")
		List<Object> keywords = (List<Object>) keywordObj;
		assertTrue(keywords.contains("Holy"), "keywords should include 'Holy': " + keywords);
		assertTrue(keywords.contains("scriptures"), "keywords should include 'scriptures': " + keywords);

		Object textObj = fieldData.get("text");
		assertTrue(textObj instanceof String);
		String text = (String) textObj;
		assertTrue(text.contains("I have no greater joy"), "The text should be returned intact");
		assertTrue(text.contains("<"),
				"The HTML should have been preserved in the stored field (although it was stripped out of the index).");

		Object chapterObj = fieldData.get("chapter");
		assertTrue(chapterObj instanceof Number);
		Number chapter = (Number) chapterObj;
		assertEquals(1, chapter.intValue());

		assertTrue(fieldData.get("add_timestamp") instanceof Number);

		Object bookObj = fieldData.get("book");
		assertTrue(bookObj instanceof String);
		assertEquals("3 John", bookObj.toString());

		Object sourceObj = fieldData.get("source");
		assertTrue(sourceObj instanceof String);
		assertEquals("kj_new.zip", sourceObj.toString());
	}

	@Test
	void testPhraseQuery() {
		PhraseQueryTutorial pqt = new PhraseQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = pqt;
		pqt.postConstruct();

		String field = "text";
		String phrase = "slow to anger";

		SearchResults slowToAnger = pqt.query(new PhraseQueryTutorialInput(field, phrase.split(" ")), 10);
		assertEquals(8l, slowToAnger.getTotalHits(), "There should be 8 results as 8 chapters mention this phrase.");
		assertEquals(8, slowToAnger.getResults().size(), "All 8 documents should be returned as we requested 10.");

		Set<String> bookChapters = bookChapters(slowToAnger);
		assertTrue(bookChapters.contains("Nehemiah 9"));
		assertTrue(bookChapters.contains("Psalms 103"));
		assertTrue(bookChapters.contains("Psalms 145"));
		assertTrue(bookChapters.contains("Proverbs 15"));
		assertTrue(bookChapters.contains("Proverbs 16"));
		assertTrue(bookChapters.contains("Joel 2"));
		assertTrue(bookChapters.contains("Jonah 4"));
		assertTrue(bookChapters.contains("Nahum 1"));

		for (int i = 0; i < slowToAnger.getResults().size(); i++) {
			String text = slowToAnger.getResults().get(i).getValues().get("text").toString();
			assertTrue(text.contains(phrase), i + ": Each result should have the phrase!");
		}
	}

	@Test
	void testSloppyPhraseQuery() {
		PhraseQueryTutorial pqt = new PhraseQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = pqt;
		pqt.postConstruct();

		String field = "text";
		String phrase = "have earth";
		int editDistance = 2;
		SearchResults haveEarth = pqt.query(new PhraseQueryTutorialInput(field, editDistance, phrase.split(" ")), 10);
		assertEquals(7, haveEarth.getTotalHits(),
				"There should be 7 results as 7 chapters mention these two words, no more than 2 intervening words.");
		assertEquals(7, haveEarth.getResults().size(), "All 7 documents should be returned as we requested 10.");
		assertEquals(5, numOccurancesInResults(haveEarth, "text", "earth have"),
				"5 of the results should contain 'earth have'.");
		assertEquals(2, numOccurancesInResults(haveEarth, "text", "have made the earth"),
				"2 of the results should contain 'have made the earth'.");
	}

	@Test
	void testMultiPhraseQuery() {
		String field = "text";

		MultiPhraseQuery.Builder b = new MultiPhraseQuery.Builder();
		b.add(new Term[] { new Term(field, "gracious"), new Term(field, "merciful") });
		b.add(new Term(field, "god"));

		MultiPhraseQueryTutorial mpqt = new MultiPhraseQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = mpqt;
		mpqt.postConstruct();

		SearchResults graciousGodOrMercifulGod = mpqt.query(b, 10);
		assertEquals(3, graciousGodOrMercifulGod.getResults().size(),
				"There should be 3 chapters with 'gracious god' or 'merciful god'");
		assertEquals(1, numOccurancesInResults(graciousGodOrMercifulGod, "text", "gracious god"),
				"There should be 1 chapter with 'gracious god'");
		assertEquals(2, numOccurancesInResults(graciousGodOrMercifulGod, "text", "merciful god"),
				"There should be 2 chapters with 'merciful god'");
	}

	@Test
	void testRangeQuery() {
		IntegerRangeQueryTutorial rqt = new IntegerRangeQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = rqt;
		rqt.postConstruct();
		IntegerRange ir = new IntegerRange("chapter", 141, 150);

		SearchResults chapters141Through150 = rqt.query(ir, 10);
		assertEquals(10l, chapters141Through150.getTotalHits(),
				"Only one Bible book has chapters numbered this high, so there should be 10 results.");
		assertEquals(10, chapters141Through150.getResults().size(),
				"We should receive all 10 documents as we requested up to 10.");
		Set<String> bookChapters = bookChapters(chapters141Through150);
		assertTrue(bookChapters.contains("Psalms 141"));
		assertTrue(bookChapters.contains("Psalms 142"));
		assertTrue(bookChapters.contains("Psalms 143"));
		assertTrue(bookChapters.contains("Psalms 144"));
		assertTrue(bookChapters.contains("Psalms 145"));
		assertTrue(bookChapters.contains("Psalms 146"));
		assertTrue(bookChapters.contains("Psalms 147"));
		assertTrue(bookChapters.contains("Psalms 148"));
		assertTrue(bookChapters.contains("Psalms 149"));
		assertTrue(bookChapters.contains("Psalms 150"));
	}

	@Test
	void testFuzzyQuery() {
		FuzzyQueryTutorial fqt = new FuzzyQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = fqt;
		fqt.postConstruct();

		assertEquals(2l, fqt.query(new FuzzyText("text", "Methuselah"), 10).getTotalHits(),
				"Should get 2 results with the name correctly-spelled, as Methuselah is mentioned in two different chapters.");
		assertEquals(2l, fqt.query(new FuzzyText("text", "Methuzelah"), 10).getTotalHits(),
				"Should get 2 results with 1 spelling error, because we use the default max edit distance of 2.");
		assertEquals(2l, fqt.query(new FuzzyText("text", "Methuselha"), 10).getTotalHits(),
				"Should get 2 results with 2 spelling errors, because we use the default max edit distance of 2.");
		assertEquals(0l, fqt.query(new FuzzyText("text", "Methuzelha"), 10).getTotalHits(),
				"Should get no results with 3 spelling errors, because we use the default max edit distance of 2.");

	}

	@Test
	void testWildcardQuery() {
		WildcardQueryTutorial wqt = new WildcardQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = wqt;
		wqt.postConstruct();

		SearchResults john123 = wqt.query(new WildcardInput("book", "? John"), 10);
		assertEquals(7l, john123.getTotalHits(), "Should get 7 chapters total.");
		assertEquals(5, numOccurancesInResultsCaseSensitive(john123, "book", "1 John"),
				"Should have 5 documents match for '1 John'");
		assertEquals(1, numOccurancesInResultsCaseSensitive(john123, "book", "2 John"),
				"Should have 1 document match for '2 John'");
		assertEquals(1, numOccurancesInResultsCaseSensitive(john123, "book", "3 John"),
				"Should have 1 document match for '3 John'");

		SearchResults joChapters = wqt.query(new WildcardInput("book", "Jo*"), 100);
		assertEquals(94l, joChapters.getTotalHits());
		assertEquals(24l, numOccurancesInResultsCaseSensitive(joChapters, "book", "Joshua"),
				"Should have 24 documents match for 'Joshua'");
		assertEquals(42l, numOccurancesInResultsCaseSensitive(joChapters, "book", "Job"),
				"Should have 42 documents match for 'Job'");
		assertEquals(3l, numOccurancesInResultsCaseSensitive(joChapters, "book", "Joel"),
				"Should have 3 documents match for 'Joel'");
		assertEquals(4l, numOccurancesInResultsCaseSensitive(joChapters, "book", "Jonah"),
				"Should have 4 documents match for 'Jonah'");
		assertEquals(21l, numOccurancesInResultsCaseSensitive(joChapters, "book", "John"),
				"Should have 21 documents match for 'John'");
	}

	@Test
	void testPrefixQuery() {
		PrefixQueryTutorial pqt = new PrefixQueryTutorial(new IndexPhysicalLocation(tempDir));
		currentTest = pqt;
		pqt.postConstruct();

		SearchResults firstCorinthians = pqt.query(new PrefixInput("book", "1 Cor"), 20);
		assertEquals(16l, firstCorinthians.getTotalHits(), "Should have 15 documents matching '1 Corinthians'");
		assertEquals(16, numOccurancesInResultsCaseSensitive(firstCorinthians, "book", "1 Corinthians"));

	}

	private Set<String> bookChapters(SearchResults searchResults) {
		return searchResults.getResults().stream()
				.map(sr -> sr.getValues().get("book") + " " + sr.getValues().get("chapter"))
				.collect(Collectors.toSet());
	}

	private int numOccurancesInResults(SearchResults sr, String field, String match) {
		return (int) sr.getResults().stream()
				.filter(r -> r.getValues().get(field).toString().toLowerCase(Locale.ROOT).contains(match)).count();
	}

	private int numOccurancesInResultsCaseSensitive(SearchResults sr, String field, String match) {
		return (int) sr.getResults().stream().filter(r -> r.getValues().get(field).toString().contains(match)).count();
	}

}
