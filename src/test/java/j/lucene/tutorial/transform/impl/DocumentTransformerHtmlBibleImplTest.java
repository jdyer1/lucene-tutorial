package j.lucene.tutorial.transform.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import j.lucene.tutorial.extract.ExtractedDocument;
import j.lucene.tutorial.transform.TransformedDocument;

class DocumentTransformerHtmlBibleImplTest {

	private DocumentTransformerHtmlBibleImpl dt;
	private Map<String, Object> rawFields;

	@BeforeEach
	void before() {
		this.rawFields = new HashMap<>();
		rawFields.put("chapter", 11);
		rawFields.put("add_timestamp", ZonedDateTime.of(2001, 2, 3, 4, 5, 6, 7, ZoneOffset.UTC));
		rawFields.put("book", "John");
		rawFields.put("source", "kj_new.zip");
		rawFields.put("synopsis", "Raising of Lazarus");
		rawFields.put("keywords", new String[] { "just", "some", "JUNK", "not very good", "keywords" });
		rawFields.put("text", raw);

		this.dt = new DocumentTransformerHtmlBibleImpl();
	}

	@AfterEach
	void after() throws Exception {
		this.dt.close();
	}

	@Test
	void test() {
		ExtractedDocument in = new ExtractedDocument(rawFields);
		TransformedDocument out = dt.transformExtractedDocument(in);
		assertNotNull(out, "There should be an extracted document.");

		List<Field> fields = out.getFields();
		assertEquals(11, fields.size(), "There should be 11 fields.");

		Field chapter = f("chapter", fields);
		assertNotNull(chapter, "There should be a field for 'chapter'");
		assertTrue(chapter instanceof IntField, "Chapter should be an IntField.");

		Field addTs = f("add_timestamp", fields);
		assertNotNull(addTs, "There should be a field for 'add_timestamp'");
		assertTrue(addTs instanceof LongField, "The timestamp should be a LongField.");

		Field book = f("book", fields);
		assertNotNull(book, "There should be a field for 'book'");
		assertTrue(book instanceof StringField, "Book should be a StringField.");

		Field source = f("source", fields);
		assertNotNull(source, "There should be a field for 'source'");
		assertTrue(source instanceof StringField, "Source should be an StringField.");

		Field synopsis = f("synopsis", fields);
		assertNotNull(synopsis, "There should be a field for 'synopsis'");
		assertTrue(synopsis instanceof TextField, "synopsis should be an TextField.");

		Field text = f("text", fields);
		assertNotNull(text, "There should be a field for 'text'");

		List<Field> keywords = fields.stream().filter(f -> {
			return f.name().equals("keywords");
		}).toList();
		assertEquals(5, keywords.size(), "Each 'keyword' should have its own Field instance");

		String[] orig = (String[]) rawFields.get("keywords");
		for (int i = 0; i < orig.length; i++) {
			String m = i + ": The keywords should preserve order, with no anlysis: case is preserved, etc.";
			assertEquals(orig[i], keywords.get(i).stringValue(), m);
			assertTrue(keywords.get(i) instanceof KeywordField, i + ": Keywords should be a KeywordField.");
		}
	}

	private Field f(String n, List<Field> l) {
		List<Field> fl = l.stream().filter(f -> {
			return f.name().equals(n);
		}).toList();
		assertEquals(1, fl.size(), "'" + n + "' should only occur once");
		return fl.iterator().next();
	}

	private static final String raw = """
			<!doctype html>
			<html lang="en">
			<head>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
			<title>Audio Books: the Book of John - Chapter 11 Raising of Lazarus</title>
			<meta name="description" content="John, chapter 11 of the King James Version of the Holy Bible - with audio narration" />
			<meta name="keywords" content="Audio, Bible, Holy, Old testament, New testament, scriptures,  salvation, faith, heaven, hell, God, Jesus" />
			<!-- Mobile viewport optimisation -->
			<link rel="shortcut icon" href="../../favicon.ico?v=2" type="image/x-icon" />
			<link href="../../apple-touch-icon.png" rel="apple-touch-icon" />
			<meta name="viewport" content="width=device-width, initial-scale=1.0" />
			<link rel="stylesheet" type="text/css" href="../_assets/css/css.css" />
			<script>
			var _gaq = _gaq || [];
			var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
			})();
			</script>
			<!--[if lt IE 9]>
			<script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
			<![endif]-->
			</head>
			<body>
			<header class="ym-noprint">
			<div id="mytop" class="ym-wrapper">
			<div class="ym-wbox">
			<span class="wp"><strong><a class="wplink" href="../../../index.htm" target="_top">W</a><a class="wplink" href="../../../index.htm" target="_top">P</a></strong></span>
			</div>
			</div>
			</header>

			<nav id="nav">
			<div class="ym-wrapper">
			<div class="ym-hlist">
			<ul>
			<li><a title="Other languages" href="https://www.wordproject.org/bibles/index.htm" target="_self">More Bibles</a></li>
			<li><a title="Audio Bibles in different languages" href="https://www.wordproject.org/bibles/audio/index.htm" target="_top">Audio Bibles</a></li>
			<!--li><a title="Search this Bible" href="search.html" target="_top">Search</a></li-->
			</ul>
			</div>
			</div>
			</nav>
			<div class="ym-wrapper ym-noprint">
			<div class="ym-wbox">

			<div class=" ym-grid">
			<div class="ym-g62 ym-gl breadCrumbs"> <a title="Bibles" href="../index.htm" target="_self">Bible</a> /  <a href="../index.htm" target="_self">KJV</a></div>
			</div>
			</div>
			</div>
			<div id="main" class="ym-clearfix" role="main">
			<div class="ym-wrapper">
			<div class="ym-wbox">

			<div class="textHeader">
			<h1>John</h1>
			<p class="ym-noprint"> Chapter:
			<a href="1.htm#0" class="chap">1</a>
			<a href="2.htm#0" class="chap">2</a>
			<a href="3.htm#0" class="chap">3</a>
			<a href="4.htm#0" class="chap">4</a>
			<a href="5.htm#0" class="chap">5</a>
			<a href="6.htm#0" class="chap">6</a>
			<a href="7.htm#0" class="chap">7</a>
			<a href="8.htm#0" class="chap">8</a>
			<a href="9.htm#0" class="chap">9</a>
			<a href="10.htm#0" class="chap">10</a>
			<span class="chapread">11</span>
			<a href="12.htm#0" class="chap">12</a>
			<a href="13.htm#0" class="chap">13</a>
			<a href="14.htm#0" class="chap">14</a>
			<a href="15.htm#0" class="chap">15</a>
			<a href="16.htm#0" class="chap">16</a>
			<a href="17.htm#0" class="chap">17</a>
			<a href="18.htm#0" class="chap">18</a>
			<a href="19.htm#0" class="chap">19</a>
			<a href="20.htm#0" class="chap">20</a>
			<a href="21.htm#0" class="chap">21</a>
			</p>
			</div>



			<div id="0" > </div>
			<div class="shareright"><a class="decreaseFont ym-button2">-</a><a class="resetFont ym-button2">Reset</a><a class="increaseFont ym-button2">+</a>
			</div>

			<div class="textOptions">
			<div class="textBody" id="textBody">
			<h3>Chapter 11 </h3>
			<!--... the Word of God:--><span class="dimver">
			</span>
			<p><span class="verse" id="1">1 </span>Now a certain man was sick, named <span class="person">Lazarus</span>, of <span class="place">Bethany</span>, the town of <span class="person">Mary </span>and her sister <span class="person">Martha</span>.
			<br /><span class="verse" id="2">2 </span>(It was that <span class="person">Mary </span>which anointed the Lord with ointment, and wiped his feet with her hair, whose brother <span class="person">Lazarus </span>was sick.)
			<br /><span class="verse" id="3">3 </span>Therefore his sisters sent unto him, saying, Lord, behold, he whom thou lovest is sick.
			<br /><span class="verse" id="4">4 </span>When <span class="person">Jesus </span>heard that, he said, <span class="word">This sickness is not unto death, but for the glory of God, that the Son of God might be glorified thereby. </span>
			<br /><span class="verse" id="57">57 </span>Now both the chief priests and the Pharisees had given a commandment, that, if any man knew where he were, he should shew it, that they might take him.
			</p>
			<!--... sharper than any twoedged sword... -->
			</div>
			</div><!-- /textOptions -->
			</div><!-- /ym-wbox end -->
			</div><!-- /ym-wrapper end -->
			</div><!-- /main -->
			<div class="ym-wrapper">
			<div class="ym-wbox">
			<div class="shareright ym-noprint">
			<!--next chapter start/Top-->
			<a class="ym-button" title="Page TOP" href="#mytop">&nbsp;<img src="../_assets/img/arrow_up.png" class="imageatt" alt="arrowup"/>&nbsp;</a>

			<a class="ym-button" title="Next chapter" href="12.htm#0">&nbsp;<img src="../_assets/img/arrow_right.png" class="imageatt" alt="arrowright"/>&nbsp;</a></p>
			<!--next chapter end-->

			</div>
			</div>
			</div>
			<footer>
			<div class="ym-wrapper">
			<div id="redborder" class="ym-wbox">
			<p class="alignCenter">Courtesy of Wordproject, a registered domain of <a href="#" target="_top">International Biblical Association</a>, a non-profit organization registered in Macau, China.      </p>
			</div>
			</footer>
			</body>
			<script src="../_assets/js/jquery-1.8.0.min.js"></script>
			<script src="../_assets/js/main.js"></script>
			</html>
			""";
}
