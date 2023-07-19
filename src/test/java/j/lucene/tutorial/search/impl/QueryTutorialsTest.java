package j.lucene.tutorial.search.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
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
		assertEquals(28l, matthew.getTotalHits());
		assertFalse(matthew.isTotalApproximate());
		assertEquals(5l, matthew.getResults().size());

		SearchResults matthewWrongCase = tqt.query(new Term("book", "matthew"), 5);
		assertEquals(0l, matthewWrongCase.getTotalHits());

		SearchResults thirdJohn = tqt.query(new Term("book", "3 John"), 5);
		assertEquals(1l, thirdJohn.getTotalHits());
		assertEquals(1l, thirdJohn.getResults().size());

		SearchResult onlyChapterIn3j = thirdJohn.getResults().iterator().next();
		Map<String, Object> fieldData = onlyChapterIn3j.getValues();
		assertEquals(2, fieldData.size());
		assertTrue(fieldData.get("keywords") instanceof String[]);
		assertTrue(fieldData.get("text") instanceof String);

	}

}
