package j.jdyer1.ingest.impl;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import j.jdyer1.ingest.IngestDocument;
import j.jdyer1.ingest.IngestDocumentProducer;
import j.jdyer1.lucenetutorial.LuceneTutorialException;

public class IngestDocumentZipProducer implements IngestDocumentProducer {

	protected static final Clock CLOCK = Clock.systemUTC();

	private static final Pattern INDEX_PATTERN = Pattern.compile("^.*title=\\\"\\[(\\d+).*>([- A-Za-z0-9]+)<.*$");

	@Override
	public Stream<IngestDocument> documents(Path zipFilePath) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(new IngestDocumentIterator(bookNameIndex(zipFilePath), zipFilePath),
						Spliterator.DISTINCT & Spliterator.IMMUTABLE & Spliterator.NONNULL),
				false);
	}

	private Map<Integer, String> bookNameIndex(Path zipFilePath) {
		try {
			Map<Integer, String> booknameByChapterId = new HashMap<>();
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()));
			ZipEntry zipEntry = null;
			while ((zipEntry = zis.getNextEntry()) != null) {
				if (zipEntry.getName().endsWith("index.htm")) {
					String[] index = new String(zis.readAllBytes()).split("\\n");
					for (String line : index) {
						Matcher m = INDEX_PATTERN.matcher(line);
						if (m.find()) {
							booknameByChapterId.put(Integer.parseInt(m.group(1)), m.group(2).trim());
						}
					}
				}
				zis.closeEntry();
			}
			zis.close();
			return Collections.unmodifiableMap(booknameByChapterId);
		} catch (Exception e) {
			throw new LuceneTutorialException(e);
		}
	}

	public static class IngestDocumentIterator implements Iterator<IngestDocument> {

		final Map<Integer, String> booknameByChapterId;
		final Path zipFilePath;
		final String source;

		private IngestDocument next = null;
		private ZipInputStream zis = null;
		private boolean done = false;

		IngestDocumentIterator(Map<Integer, String> booknameByChapterId, Path zipFilePath) {
			this.booknameByChapterId = booknameByChapterId;
			this.zipFilePath = zipFilePath;
			this.source = zipFilePath.getFileName().toString();
		}

		@Override
		public boolean hasNext() {
			advance();
			return next != null;
		}

		@Override
		public IngestDocument next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			IngestDocument val = next;
			next = null;
			return val;
		}

		private void advance() {
			if (done) {
				return;
			}
			if (next != null) {
				return;
			}
			if (zis == null) {
				try {
					this.zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()));
				} catch (Exception e) {
					throw new LuceneTutorialException(e);
				}
			}
			ZipEntry zipEntry = null;
			try {
				while ((zipEntry = zis.getNextEntry()) != null) {
					maybePopulateNext(zipEntry);
					zis.closeEntry();
					if (next != null) {
						return;
					}
				}
				zis.close();
				done = true;
			} catch (Exception e) {
				throw new LuceneTutorialException(e);
			}
		}

		private void maybePopulateNext(ZipEntry zipEntry) {
			if (zipEntry.getName().endsWith(".htm")) {
				Integer chapter = chapter(zipEntry);
				if (chapter != null) {
					Map<String, Object> fields = new HashMap<>();
					fields.put("chapter", chapter);

					String bookStr = Paths.get(zipEntry.getName()).getName(1).toString();
					fields.put("book", booknameByChapterId.get(Integer.parseInt(bookStr)));

					try {
						fields.put("text", new String(zis.readAllBytes()));
					} catch (Exception e) {
						throw new LuceneTutorialException(e);
					}
					fields.put("add_timestamp", ZonedDateTime.now(CLOCK));
					fields.put("source", source);
					next = new IngestDocument(fields);
				}
			}
		}

		private Integer chapter(ZipEntry ze) {
			String chapterStr = Paths.get(ze.getName()).getFileName().toString();
			int indexOfDot = chapterStr.lastIndexOf('.');
			chapterStr = chapterStr.substring(0, indexOfDot);
			try {
				return Integer.parseInt(chapterStr);
			} catch (Exception e) {
				return null;
			}
		}

	}

}
