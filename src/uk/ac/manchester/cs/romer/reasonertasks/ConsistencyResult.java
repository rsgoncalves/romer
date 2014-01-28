package uk.ac.manchester.cs.romer.reasonertasks;

/**
 * @author Rafael S. Goncalves
 * Information Management Group (IMG)
 * School of Computer Science
 * University of Manchester
 */
public class ConsistencyResult {
	private boolean cons;
	private double consTime;
	
	/**
	 * Constructor
	 * @param cons	True if consistent
	 * @param consTime	Consistency checking time (in seconds)
	 */
	public ConsistencyResult(boolean cons, double consTime) {
		this.cons = cons;
		this.consTime = consTime;
	}
	
	
	/**
	 * @return true if consistent, false otherwise
	 */
	public boolean isConsistent() {
		return cons;
	}
	
	
	/**
	 * @return consistency checking time (in seconds)
	 */
	public double getTime() {
		return consTime;
	}
}
