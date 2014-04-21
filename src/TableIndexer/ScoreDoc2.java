package TableIndexer;

import org.apache.lucene.search.ScoreDoc;

public class ScoreDoc2 extends ScoreDoc{

	public int numOfDocs = 0;
	
	public ScoreDoc2(int doc, float score) {
		super(doc, score);
		numOfDocs = 1;
		// TODO Auto-generated constructor stub
	}
	public ScoreDoc2(ScoreDoc sd) {
		super(sd.doc, sd.score);
		numOfDocs = 1;
		// TODO Auto-generated constructor stub
	}
	
	

}
