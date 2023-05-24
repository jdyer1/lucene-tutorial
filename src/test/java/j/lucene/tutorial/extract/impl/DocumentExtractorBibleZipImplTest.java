package j.lucene.tutorial.extract.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentExtractorBibleZipImplTest {

	private DocumentExtractorBibleZipImpl cii;
	private Path zipPath;

	@BeforeEach
	void before() {
		this.cii = new DocumentExtractorBibleZipImpl();

		this.zipPath = Paths.get(".").toAbsolutePath().normalize().resolve("src").resolve("test").resolve("resources")
				.resolve("kj_new.zip");
	}

	@Test
	void test() {
		Pattern uppercasePattern = Pattern.compile("[A-Z]");

		AtomicInteger i = new AtomicInteger();
		AtomicInteger numSynopsis = new AtomicInteger();
		Set<String> bookchapters = new HashSet<>();
		cii.documentsFromFilePath(zipPath).forEach(doc -> {

			i.incrementAndGet();

			String m = i + ": should record the source filename on each document";
			assertEquals("kj_new.zip", doc.getContentByFieldname().get("source"), m);

			m = i + ": should record the timestamp";
			assertTrue(doc.getContentByFieldname().get("add_timestamp") instanceof ZonedDateTime, m);

			String book = doc.getContentByFieldname().get("book").toString();
			m = i + ": should record the book name";
			assertNotNull(book, m);

			m = i + ": The shortest book name is 3 letters long: " + book;
			assertTrue(book.length() > 2, m);

			m = i + ": No book name should be as long as 100 characters: " + book.length();
			assertTrue(book.length() < 100, m);

			m = i + ": should record the chapter number";
			assertTrue(doc.getContentByFieldname().get("chapter") instanceof Integer);
			Integer chapter = (Integer) doc.getContentByFieldname().get("chapter");
			m = i + ": The chapters should be greater than zero";
			assertTrue(chapter > 0, m);
			m = i + ": The chapters should be less than 151";
			assertTrue(chapter < 151, m);

			m = i + ": The book/chapter combination should be unique for each document.";
			assertTrue(bookchapters.add(book + "-" + chapter));

			m = i + ": There should not be any lossy tranformations to the text at this stage";
			String text = doc.getContentByFieldname().get("text").toString();
			assertTrue(text.contains("<span"), (m + "; we should preserve html."));
			assertTrue(uppercasePattern.matcher(text).find(), (m + "; we should preserve case."));

			m = i + ": There should be a short synopsis";
			if (doc.getContentByFieldname().get("synopsis") != null) {
				numSynopsis.incrementAndGet();
				String synopsis = doc.getContentByFieldname().get("synopsis").toString();
				m = i + ": The synopsis should at least be 5 long";
				assertTrue(synopsis.length() > 5, m);
				m = i + ": The synopsis should omit the prelude 'Audio Books...'";
				assertFalse(synopsis.startsWith("Audio Books: the Book of"), m);
				m = i + ": The synopsis should strip out the chapter information.";
				assertFalse(synopsis.contains("- Chapter"));
			}

			m = i + ": There should be an array of keywords.";
			assertTrue(doc.getContentByFieldname().get("keywords") instanceof String[], m);
			String[] keywords = (String[]) doc.getContentByFieldname().get("keywords");
			m = i + ": There should be at least 2 keywords";
			assertTrue(keywords.length > 1, m);
			m = i + ": The keyword 'Audio' should be stripped out.";
			assertFalse(Arrays.stream(keywords).map(String::toLowerCase).anyMatch(k -> k.equals("audio")), m);
		});

		assertEquals(1189, i.get(), "There should be 1,189 documents corresponding to each Bible chapter.");
		assertEquals(189, numSynopsis.get(), "There should be 189 documents with a synopsis.");
	}
}
