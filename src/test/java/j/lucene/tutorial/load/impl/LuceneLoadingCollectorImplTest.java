package j.lucene.tutorial.load.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PrintStreamInfoStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import j.lucene.tutorial.extract.ExtractedDocument;
import j.lucene.tutorial.transform.DocumentTransformer;
import j.lucene.tutorial.transform.TransformedDocument;

class LuceneLoadingCollectorImplTest {

	private LuceneLoadingCollectorImpl llc;
	private MockDocumentTransformer mdf;
	private ExtractedDocument ed;
	private Path tempDir;

	@BeforeEach
	void before() throws Exception {
		this.tempDir = Files.createTempDirectory(this.getClass().getSimpleName());
		this.llc = new LuceneLoadingCollectorImpl(new IndexPhysicalLocation(tempDir));
		llc.iwc = new IndexWriterConfig();
		llc.iwc.setCodec(new SimpleTextCodec());
		llc.iwc.setInfoStream(new PrintStreamInfoStream(System.out));
		llc.postConstruct();

		this.ed = new ExtractedDocument(Collections.emptyMap());

		this.mdf = new MockDocumentTransformer();

	}

	@AfterEach
	void after() throws Exception {
		llc.preDestroy();
		Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

	@Test
	void test() throws Exception {
		List<ExtractedDocument> l = List.of(ed, ed, ed, ed, ed);
		Long numIndexed = l.stream().map(mdf::transformExtractedDocument).collect(llc);
		assertEquals(5, numIndexed);
		assertEquals(ed, mdf.lastExtractedDocument);

		long numFiles = Files.list(tempDir).collect(Collectors.counting());
		assertEquals(9, numFiles, "should have created 9 files for a single-segment index");

		assertTrue(fileContents(".si").contains("number of documents 5"),
				"should say there are 5 document in the 'si' file.");

		assertTrue(fileContents(".inf").contains("number of fields 3"),
				"should say there are 3 fields in the 'inf' file.");

		assertEquals(5l, Arrays.stream(fileContents(".fld").split("\\b")).filter(s -> s.equals("doc")).count(),
				"should list 5 documents in the 'fld' file.");

		assertTrue(fileContents(".dim").contains("point count 5"), "should say there are 5 points in the 'dim' file.");

		assertTrue(fileContents(".dii").contains("field fp name chapter"),
				"should list field 'chapter' in the 'dii' file.");

		assertTrue(fileContents(".pst").contains("term kj_new.zip"),
				"should list term 'kj_new.zip' in the 'pst' file.");

		assertTrue(fileContents(".dat").contains("field chapter"), "should list field 'chapter' in the 'dat' file.");
		assertTrue(fileContents(".dat").contains("field source"), "should list field 'source' in the 'dat' file.");
		
		assertFalse(llc.firstFailure().isPresent());

	}

	private String fileContents(String ext) throws IOException {
		Optional<Path> pO = Files.list(tempDir).filter(p -> p.getFileName().toString().endsWith(ext)).findFirst();
		if (pO.isPresent()) {
			return new String(Files.readAllBytes(pO.get()));
		}
		return null;
	}

	public class MockDocumentTransformer implements DocumentTransformer {

		public int counter = 0;

		public ExtractedDocument lastExtractedDocument = null;

		@Override
		public TransformedDocument transformExtractedDocument(ExtractedDocument in) {
			this.lastExtractedDocument = in;

			List<Field> fields = new ArrayList<>();
			fields.add(new IntField("chapter", ++counter, Store.NO));
			fields.add(new NumericDocValuesField("chapter_df", counter));
			fields.add(new StringField("source", "kj_new.zip", Store.NO));
			fields.add(new SortedDocValuesField("source", new BytesRef("kj_new.zip")));

			return new TransformedDocument(fields);
		}

	}

}
