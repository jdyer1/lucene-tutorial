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
import java.util.Map;

import org.apache.lucene.index.Term;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import j.lucene.tutorial.extract.impl.DocumentExtractorBibleZipImpl;
import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.load.impl.LuceneLoadingCollectorImpl;
import j.lucene.tutorial.search.SearchResult;
import j.lucene.tutorial.search.SearchResults;
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
		assertEquals(2, fieldData.size(),
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
	}

}
