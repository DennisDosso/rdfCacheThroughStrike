package utils;

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

	public int strikes;

	public boolean present;

	public ReturnBox() {
		this.resultSetSize = 0;
		this.inTime = true;
	}
}
