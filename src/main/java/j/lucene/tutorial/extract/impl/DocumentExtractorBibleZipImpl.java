package j.lucene.tutorial.extract.impl;

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

import j.lucene.tutorial.LuceneTutorialException;
import j.lucene.tutorial.extract.DocumentExtractor;
import j.lucene.tutorial.extract.ExtractedDocument;

/**
 * This Document Extractor gets raw HTML pages, each representing a Bible
 * chapter. A very specific zip archive format is assumed. The zip archive
 * represents our System Of Record, where we can get the data anytime we need to
 * recreate the Lucene index.
 *
 */
public class DocumentExtractorBibleZipImpl implements DocumentExtractor {

	/**
	 * The added timestamp is UTC
	 */
	protected static final Clock CLOCK = Clock.systemUTC();

	private static final Pattern INDEX_PATTERN = Pattern.compile("^.*title=\\\"\\[(\\d+).*>([- A-Za-z0-9]+)<.*$");
	private static final Pattern SYNOPSIS_PATTERN1 = Pattern.compile("^.*<title>Audio.Books:.*\\\\d{1,3}.(.*)<.*$");
	private static final Pattern SYNOPSIS_PATTERN2 = Pattern.compile("^.*<title>(.*), .*<.*$");
	private static final Pattern KEYWORDS_PATTERN = Pattern.compile("^.*<meta name=\\\"keywords\\\" content=\\\"(.*)\\\".*$");
	private static final Pattern KEYWORD_SPLIT_PATTERN = Pattern.compile(", ");
	private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\\n");

	@Override
	public Stream<ExtractedDocument> documentsFromFilePath(Path zipFilePath) {
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

	/**
	 * Iterator, used internally to provide the Stream
	 *
	 */
	public static class IngestDocumentIterator implements Iterator<ExtractedDocument> {

		final Map<Integer, String> booknameByChapterId;
		final Path zipFilePath;
		final String source;

		private ExtractedDocument next = null;
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
		public ExtractedDocument next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			ExtractedDocument val = next;
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
						String text = new String(zis.readAllBytes());
						String[] textLines = NEWLINE_PATTERN.split(text);
						maybeAddSynopsisAndKeywords(fields, textLines);
						fields.put("text", text);
					} catch (Exception e) {
						throw new LuceneTutorialException(e);
					}
					fields.put("add_timestamp", ZonedDateTime.now(CLOCK));
					fields.put("source", source);
					next = new ExtractedDocument(fields);
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

		private void maybeAddSynopsisAndKeywords(Map<String, Object> fields, String[] textLines) {
			boolean foundSynopsis = false;
			boolean foundKeywords = false;
			for (String line : textLines) {
				if (!foundSynopsis) {
					foundSynopsis = synopsis(fields, line);
					if(foundSynopsis) {
						continue;
					}
				}
				foundKeywords = keywords(fields, line);
				if (foundKeywords && foundSynopsis) {
					return;
				}
			}
		}
		
		private boolean synopsis(Map<String, Object> fields, String line) {
			Matcher synopsisM = SYNOPSIS_PATTERN1.matcher(line);
			if (synopsisM.find()) {
				fields.put("synopsis", synopsisM.group(1));
				return true;
			}
			synopsisM = SYNOPSIS_PATTERN2.matcher(line);
			if (synopsisM.find()) {
				fields.put("synopsis", synopsisM.group(1));
				return true;
			}
			return false;
		}
		private boolean keywords(Map<String, Object> fields, String line) {
			Matcher keywordM = KEYWORDS_PATTERN.matcher(line);
			if (keywordM.find()) {
				String[] kArr = KEYWORD_SPLIT_PATTERN.split(keywordM.group(1));
				if(kArr[0].equals("Audio")) {
					String[] kArr1 = new String[kArr.length-1];
					System.arraycopy(kArr, 1, kArr1, 0, kArr1.length);
					kArr = kArr1;
				}
				fields.put("keywords", kArr);
				return true;
			}
			return false;
		}
	}

}
