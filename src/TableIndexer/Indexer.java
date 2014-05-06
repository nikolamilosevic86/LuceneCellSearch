package TableIndexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.InputSource;

public class Indexer {

	public static void performIndexing(String docsPath, String indexPath,
			boolean create) {

		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out
					.println("Document directory '"
							+ docDir.getAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(new File(indexPath));
			// :Post-Release-Update-Version.LUCENE_XY:
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47,
					analyzer);

			if (create) {
				// Create a new index in the directory, removing any
				// previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);

			IndexWriter writer = new IndexWriter(dir, iwc);
			Indexer.indexDocs(writer, docDir);

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here. This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime()
					+ " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
		}
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is
	 * given, recurses over files and directories found under the given
	 * directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For
	 * good throughput, put multiple documents into your input file(s). An
	 * example of this is in the benchmark module, which can create "line doc"
	 * files, one document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be
	 *            stored
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 */
	static void indexDocs(IndexWriter writer, File file) throws IOException {
		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(writer, new File(file, files[i]));
					}
				}
			} else {

				// try {
				// fis = new FileInputStream(file);
				// } catch (FileNotFoundException fnfe) {
				// // at least on windows, some temporary files raise this
				// exception with an "access denied" message
				// // checking if the file can be read doesn't help
				// return;
				// }

				try {

					// make a new, empty document
					Document doc = new Document();

					// Add the path of the file as a field named "path". Use a
					// field that is indexed (i.e. searchable), but don't
					// tokenize
					// the field into separate words and don't index term
					// frequency
					// or positional information:
					Field pathField = new StringField("path", file.getPath(),
							Field.Store.YES);
					doc.add(pathField);

					// Add the last modified date of the file a field named
					// "modified".
					// Use a LongField that is indexed (i.e. efficiently
					// filterable with
					// NumericRangeFilter). This indexes to milli-second
					// resolution, which
					// is often too fine. You could instead create a number
					// based on
					// year/month/day/hour/minutes/seconds, down the resolution
					// you require.
					// For example the long value 2011021714 would mean
					// February 17, 2011, 2-3 PM.
					doc.add(new LongField("modified", file.lastModified(),
							Field.Store.NO));

					@SuppressWarnings("resource")
					BufferedReader reader = new BufferedReader(new FileReader(
							file.getPath()));
					String line = null;
					String xml = "";
					while ((line = reader.readLine()) != null) {
						if (line.contains("JATS-archivearticle1.dtd")
								|| line.contains("archivearticle.dtd"))
							continue;
						xml += line + '\n';
					}
					String attribute = "";
					String value = "";
					String documentTitle = "";
					String tableTitle = "";
					String tableFooter = "";
					String PMC = "";
					String tableOrder = "";
					try {
						DocumentBuilderFactory factory = DocumentBuilderFactory
								.newInstance();
						factory.setNamespaceAware(true);
						factory.setValidating(false);
						DocumentBuilder builder = factory.newDocumentBuilder();
						InputSource is = new InputSource(new StringReader(xml));
						org.w3c.dom.Document parse = builder.parse(is);
						attribute = parse.getElementsByTagName("attribute")
								.item(0).getTextContent();
						value = parse.getElementsByTagName("value").item(0)
								.getTextContent();
						tableTitle = parse.getElementsByTagName("tableName")
								.item(0).getTextContent();
						tableFooter = parse.getElementsByTagName("tableFooter")
								.item(0).getTextContent();
						documentTitle = parse
								.getElementsByTagName("DocumentTitle").item(0)
								.getTextContent();
						PMC = parse.getElementsByTagName("PMC").item(0)
								.getTextContent();
						tableOrder = parse.getElementsByTagName("tableOrder").item(0)
								.getTextContent();

					} catch (Exception ex) {
						ex.printStackTrace();
					}
					// Add the contents of the file to a field named "contents".
					// Specify a Reader,
					// so that the text of the file is tokenized and indexed,
					// but not stored.
					// Note that FileReader expects the file to be in UTF-8
					// encoding.
					// If that's not the case searching for special characters
					// will fail.

					TextField att = new TextField("attribute", attribute,
							Field.Store.YES);
					att.setBoost(1.1f);
					TextField val = new TextField("value", value,
							Field.Store.YES);
					val.setBoost(1.3f);
					TextField tabname = new TextField("tableName", tableTitle,
							Field.Store.YES);
					tabname.setBoost(0.7f);
					TextField tabfoot = new TextField("tableFooter",
							tableFooter, Field.Store.YES);
					tabfoot.setBoost(0.7f);
					TextField tableOrd = new TextField("tableOrder",
							tableOrder, Field.Store.YES);
					TextField doctitle = new TextField("DocumentTitle",
							documentTitle, Field.Store.YES);
					doctitle.setBoost(0.5f);
					TextField pmc = new TextField("PMC", PMC, Field.Store.YES);
					pmc.setBoost(1.0f);

					doc.add(att);
					doc.add(val);
					doc.add(tabname);
					doc.add(tabfoot);
					doc.add(doctitle);
					doc.add(pmc);
					doc.add(tableOrd);

					if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
						// New index, so we just add the document (no old
						// document can be there):
						System.out.println("adding " + file);
						writer.addDocument(doc);
					} else {
						// Existing index (an old copy of this document may have
						// been indexed) so
						// we use updateDocument instead to replace the old one
						// matching the exact
						// path, if present:
						System.out.println("updating " + file);
						writer.updateDocument(new Term("path", file.getPath()),
								doc);
					}

				} finally {
					// fis.close();
				}
			}
		}
	}
}
