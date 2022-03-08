package utils;

import java.util.ArrayList;
import java.util.List;

/** Class that contains data that we may return from methods
 * */
public class ReturnBox {
	
	/** time spent in an execution*/
	public long time;

	public long queryTime;

	/** Set this to true if we completed our execution in time
	 * */
	public boolean inTime;

	public long nanoTime;
	
	public int size;

	public int resultSetSize;
	
	public boolean foundSomething = false;

	public List<String[]> lineage;
	public List<String> results;

	public int strikes;

	public boolean present;

	public boolean malformed;

	public ReturnBox() {
		this.resultSetSize = 0;
		this.inTime = true;
		malformed = false;
		results = new ArrayList<>();
	}

	public String resultsToString() {
		String s = "";
		for(int i = 0; i < results.size() - 1; ++i)
			s = s + results.get(i) + ",";
		s = s + results.get(results.size() - 1);
		return s;
	}
}
