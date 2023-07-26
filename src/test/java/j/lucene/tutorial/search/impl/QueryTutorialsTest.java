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
import j.lucene.tutorial.search.impl.PhraseQueryTutorial.PhraseQueryTutorialInput;
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
		assertEquals(3, graciousGodOrMercifulGod.getResults().size(), "There should be 3 chapters with 'gracious god' or 'merciful god'");
		assertEquals(1,  numOccurancesInResults(graciousGodOrMercifulGod, "text", "gracious god"), "There should be 1 chapter with 'gracious god'");
		assertEquals(2,  numOccurancesInResults(graciousGodOrMercifulGod, "text", "merciful god"), "There should be 2 chapters with 'merciful god'");
		
		
		
		
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

}
