package TableIndexer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class SearchMain {

	/**
	 * Index all text files under a directory.
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException {
		String usage = "java LuceneTableSearch.jar"
				+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
				+ "in INDEX_PATH that can be searched with SearchFiles";
		String indexPath = "index";
		String docsPath = null;
		boolean create = true;
		if (!Arrays.asList(args).contains("-s")) {
			for (int i = 0; i < args.length; i++) {
				if ("-index".equals(args[i])) {
					indexPath = args[i + 1];
					i++;
				} else if ("-docs".equals(args[i])) {
					docsPath = args[i + 1];
					i++;
				} else if ("-update".equals(args[i])) {
					create = false;
				}
			}

			if (docsPath == null) {
				System.err.println("Usage: " + usage);
				System.exit(1);
			}
			Indexer.performIndexing(docsPath, indexPath, create);
		} else {
			String index = "index";
			String field = "contents";
			String queries = null;
			int repeat = 0;
			boolean raw = false;
			String queryString = null;
			int hitsPerPage = 10;

			for (int i = 0; i < args.length; i++) {
				if ("-index".equals(args[i])) {
					index = args[i + 1];
					i++;
				} else if ("-field".equals(args[i])) {
					field = args[i + 1];
					i++;
				} else if ("-queries".equals(args[i])) {
					queries = args[i + 1];
					i++;
				} else if ("-query".equals(args[i])) {
					queryString = args[i + 1];
					i++;
				} else if ("-repeat".equals(args[i])) {
					repeat = Integer.parseInt(args[i + 1]);
					i++;
				} else if ("-raw".equals(args[i])) {
					raw = true;
				} else if ("-paging".equals(args[i])) {
					hitsPerPage = Integer.parseInt(args[i + 1]);
					if (hitsPerPage <= 0) {
						System.err
								.println("There must be at least 1 hit per page.");
						System.exit(1);
					}
					i++;
				}
			}
			Searcher.PerformSearch(queries, index, queryString, repeat,
					hitsPerPage, raw, field);

		}
	}

}