package TableIndexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Searcher {

	/**
	 * This demonstrates a typical paging search scenario, where the search
	 * engine presents pages of size n to the user. The user can then go to the
	 * next page if interested in the next hits.
	 * 
	 * When the query is executed for the first time, then only enough results
	 * are collected to fill 5 result pages. If the user wants to page beyond
	 * this limit, then the query is executed another time and all hits are
	 * collected.
	 * 
	 */
	public static void doPagingSearch(BufferedReader in,
			IndexSearcher searcher, Query query, int hitsPerPage, boolean raw,
			boolean interactive) throws IOException {

		// Collect enough docs to show 5 pages
		TopDocs results = searcher.search(query, 5000 * hitsPerPage);
		ScoreDoc[] hits = results.scoreDocs;

		int numTotalHits = results.totalHits;
		//System.out.println(numTotalHits + " total matching documents");

		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);

		while (true) {
			if (end > hits.length) {
				System.out
						.println("Only results 1 - " + hits.length + " of "
								+ numTotalHits
								+ " total matching documents collected.");
				System.out.println("Collect more (y/n) ?");
				String line = in.readLine();
				if (line.length() == 0 || line.charAt(0) == 'n') {
					break;
				}

				hits = searcher.search(query, numTotalHits).scoreDocs;
			}
			HashMap<String,Document> res = new HashMap<String,Document>();
			HashMap<String,ScoreDoc2> scoreres = new HashMap<String,ScoreDoc2>();
			for(ScoreDoc hit : hits)
			{
				Document doc = searcher.doc(hit.doc);
				String pmc = doc.get("PMC");
				String tableOrder = doc.get("tableOrder");
				
				if(res.get(pmc+tableOrder)==null)
				{
					res.put(pmc+tableOrder, doc);
					scoreres.put(pmc+tableOrder, new ScoreDoc2(hit));
				}
				else
				{
					ScoreDoc2 score = (ScoreDoc2) scoreres.get(pmc+tableOrder);
					score.score+=hit.score;
					score.numOfDocs++;
				}
			}
			int k= 0;
			hits = new ScoreDoc[scoreres.size()];
			for (Entry<String, ScoreDoc2> mapEntry : scoreres.entrySet()) {
				hits[k] = mapEntry.getValue();
				hits[k].score = (float) (hits[k].score / (mapEntry.getValue().numOfDocs*0.9));
			    k++;
			}
			System.out.println(k + " total matching documents");
			k = 0;
			List<ScoreDoc> arr =  Arrays.asList(hits);
			Collections.sort(arr, new Comparator<ScoreDoc>() {
				   public int compare(ScoreDoc b1, ScoreDoc b2) {
					   if(b1.score>b2.score) return -1;
					   else return 1;
				   }
				});
			k=0;
			hits = new ScoreDoc[arr.size()];
			for(ScoreDoc ar :arr)
			{
				hits[k] = ar;
				k++;
			}
			end = Math.min(hits.length, start + hitsPerPage);

			for (int i = start; i < end; i++) {
				if (raw) { // output raw format
					System.out.println("doc=" + hits[i].doc + " score="
							+ hits[i].score);
					continue;
				}

				Document doc = searcher.doc(hits[i].doc);
				String path = doc.get("path");
				if (path != null) {
					System.out.println((i + 1) + ". " + path);
					String pmc = doc.get("PMC");
					if(pmc!=null)
					{
						System.out.println("	PMC:"+doc.get("PMC"));
					}
					String tableNo = doc.get("tableOrder");
					if(tableNo!=null)
					{
						System.out.println("	Table:"+doc.get("tableOrder"));
					}
					String title = doc.get("tableName");
					if (title != null) {
						System.out.println("   tableName: " + doc.get("tableName"));
					}
				} else {
					System.out.println((i + 1) + ". "
							+ "No path for this document");
				}

			}

			if (!interactive || end == 0) {
				break;
			}

			if (numTotalHits >= end) {
				boolean quit = false;
				while (true) {
					System.out.print("Press ");
					if (start - hitsPerPage >= 0) {
						System.out.print("(p)revious page, ");
					}
					if (start + hitsPerPage < numTotalHits) {
						System.out.print("(n)ext page, ");
					}
					System.out
							.println("(q)uit or enter number to jump to a page.");

					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'q') {
						quit = true;
						break;
					}
					if (line.charAt(0) == 'p') {
						start = Math.max(0, start - hitsPerPage);
						break;
					} else if (line.charAt(0) == 'n') {
						if (start + hitsPerPage < numTotalHits) {
							start += hitsPerPage;
						}
						break;
					} else {
						int page = Integer.parseInt(line);
						if ((page - 1) * hitsPerPage < numTotalHits) {
							start = (page - 1) * hitsPerPage;
							break;
						} else {
							System.out.println("No such page");
						}
					}
				}
				if (quit)
					break;
				end = Math.min(numTotalHits, start + hitsPerPage);
			}
		}
	}

	
	public static void PerformSearch(String queries, String index,
			String queryString, int repeat, int hitsPerPage, boolean raw,
			String field) throws IOException, ParseException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
				index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		// :Post-Release-Update-Version.LUCENE_XY:
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);

		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					queries), "UTF-16"));
		} else {
			in = new BufferedReader(new InputStreamReader(System.in, "UTF-16"));
		}
		// :Post-Release-Update-Version.LUCENE_XY:

		MultiFieldQueryParser parser = new MultiFieldQueryParser(
				Version.LUCENE_47, new String[] { "attribute", "value",
						"tableName", "tableFooter", "DocumentTitle" }, analyzer);
		while (true) {
			if (queries == null && queryString == null) { // prompt the user
				System.out.println("Enter query: ");
			}

			String line = queryString != null ? queryString : in.readLine();

			if (line == null || line.length() == -1) {
				break;
			}
			line = line.trim();
			if (line.length() == 0) {
				break;
			}
			Query query = parser.parse(line);
			System.out.println("Searching for: " + query.toString(field));

			if (repeat > 0) { // repeat & time as benchmark
				Date start = new Date();
				for (int i = 0; i < repeat; i++) {
					searcher.search(query, null, 100);
				}
				Date end = new Date();
				System.out.println("Time: " + (end.getTime() - start.getTime())
						+ "ms");
			}
			Searcher.doPagingSearch(in, searcher, query, hitsPerPage, raw,
					queries == null && queryString == null);

			if (queryString != null) {
				break;
			}
		}
		reader.close();
	}
}
