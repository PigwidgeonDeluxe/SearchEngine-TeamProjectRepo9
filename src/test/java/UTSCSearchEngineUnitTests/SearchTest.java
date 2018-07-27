package UTSCSearchEngineUnitTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;

import UTSCSearchEngine.Database;
import UTSCSearchEngine.Indexing;
import UTSCSearchEngine.Search;

public class SearchTest {

	private String url = "jdbc:sqlite:test-database.db";

	@Before
	public void setUp() throws SQLException, IOException {

		Database db = new Database(this.url);
		Connection con = db.connect();

		// cleanup
		String dropTable = "DROP TABLE IF EXISTS file";
		PreparedStatement pstmt1 = con.prepareStatement(dropTable);
		pstmt1.executeUpdate();
		pstmt1.close();

		// create table
		String createTable = "CREATE TABLE file (id integer primary key autoincrement, file blob, "
				+ "file_name text, file_type text, file_size integer, uploader_name text, uploader_type "
				+ "text, uploaded_on integer)";
		PreparedStatement pstmt2 = con.prepareStatement(createTable);
		pstmt2.executeUpdate();
		pstmt2.close();

		// insert a sample txt file
		byte[] fileContent = "this is some sample text".getBytes(Charset.forName("UTF-8"));
		String txtFileName = "text file.txt";
		String txtFileType = "txt";
		String uploaderName = "test user";
		String uploaderType = "student";
		db.insertFileData(fileContent, txtFileName, txtFileType, uploaderName, uploaderType, null);

		// insert a sample doc file
		byte[] docContent = TestUtils.createDocFile("this is some sample text").toByteArray();
		String docFileName = "word file.docx";
		String docFileType = "docx";
		db.insertFileData(docContent, docFileName, docFileType, uploaderName, uploaderType, null);

		// insert an HTML file
		byte[] htmlContent = ("<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n"
				+ "<h1>My First Heading</h1>\n<p>my first paragraph</p>\n" + "\n" + "</body>\n" + "</html>")
						.getBytes(Charset.forName("UTF-8"));
		String htmlFileName = "html file.html";
		String htmlFileType = "html";
		db.insertFileData(htmlContent, htmlFileName, htmlFileType, uploaderName, uploaderType, null);

		// insert a PDF file
		byte[] pdfContent = TestUtils.createPdfFile("this is some sample text").toByteArray();
		String pdfFileName = "pdf file.pdf";
		String pdfFileType = "pdf";
		db.insertFileData(pdfContent, pdfFileName, pdfFileType, uploaderName, uploaderType, null);
	}

	@Test
	public void testDoIndexingTxt() throws IOException {

		Search search = new Search();

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		when(mockRequest.getParameter("fileType")).thenReturn("txt");
		when(mockResponse.getWriter()).thenReturn(printWriter);

		search.callIndexing(this.url);
		search.doGet(mockRequest, mockResponse);

		stringWriter.flush();
		assertTrue(stringWriter.toString().contains("this is some sample text"));
	}

	@Test
	public void testDoIndexingDoc() throws IOException {

		Search search = new Search();

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		when(mockRequest.getParameter("fileType")).thenReturn("docx");
		when(mockResponse.getWriter()).thenReturn(printWriter);

		search.callIndexing(this.url);
		search.doGet(mockRequest, mockResponse);

		stringWriter.flush();
		assertTrue(stringWriter.toString().contains("this is some sample text"));
	}

	@Test
	public void testDoIndexingPdf() throws IOException {

		Search search = new Search();

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		when(mockRequest.getParameter("fileType")).thenReturn("pdf");
		when(mockResponse.getWriter()).thenReturn(printWriter);

		search.callIndexing(this.url);
		search.doGet(mockRequest, mockResponse);

		stringWriter.flush();
		assertTrue(stringWriter.toString().contains("this is some sample text"));
	}

	@Test
	public void testDoIndexingHtml() throws IOException {

		Search search = new Search();

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		when(mockRequest.getParameter("fileType")).thenReturn("html");
		when(mockResponse.getWriter()).thenReturn(printWriter);

		search.callIndexing(this.url);
		search.doGet(mockRequest, mockResponse);

		stringWriter.flush();
		assertTrue(stringWriter.toString().contains("my first paragraph"));
	}

	@Test
	public void testMultipleOptions() throws IOException {

		Search search = new Search();

		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		when(mockRequest.getParameter("fileType")).thenReturn("html");
		when(mockRequest.getParameter("fileName")).thenReturn("pdf");
		when(mockResponse.getWriter()).thenReturn(printWriter);

		search.callIndexing(this.url);
		search.doGet(mockRequest, mockResponse);

		stringWriter.flush();
		assertTrue(stringWriter.toString().contains("my first paragraph"));
		assertTrue(stringWriter.toString().contains("pdf file.pdf"));
	}

	@Test
	public void testGetIndex()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Indexing indexer = new Indexing();
		Directory testDir = new RAMDirectory();
		Field field = indexer.getClass().getDeclaredField("index");
		field.setAccessible(true);
		field.set(indexer, testDir);
		final Directory result = indexer.getIndex();

		assertEquals("Directory wasn't retrieved properly", testDir, result);
	}

	@Test
	public void testGetAnalyzer()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Indexing indexer = new Indexing();
		StandardAnalyzer testAnalyzer = new StandardAnalyzer();
		Field field = indexer.getClass().getDeclaredField("analyzer");
		field.setAccessible(true);
		field.set(indexer, testAnalyzer);
		final StandardAnalyzer result = indexer.getAnalyzer();

		assertEquals("Analyzer wasn't retrieved properly", testAnalyzer, result);
	}

	@Test
	public void testGetDocDir()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Indexing indexer = new Indexing();
		Path testPath = null;
		Field field = indexer.getClass().getDeclaredField("analyzer");
		field.setAccessible(true);
		field.set(indexer, testPath);
		final Path result = indexer.getDocDir();

		assertEquals("Path wasn't retrieved properly", testPath, result);
	}
}