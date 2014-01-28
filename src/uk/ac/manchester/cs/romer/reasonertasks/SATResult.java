package uk.ac.manchester.cs.romer.reasonertasks;

import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * @author Rafael S. Goncalves
 * Information Management Group (IMG)
 * School of Computer Science
 * University of Manchester
 */
public class SATResult {
	private OWLClassExpression c;
	private boolean sat;
	private String reasonerName;
	private Double satTime, elapsedTime;
	
	/**
	 * Constructor
	 * @param c	Concept
	 * @param sat	isSatisfiable
	 * @param satTime	SAT checking time (in seconds)
	 */
	public SATResult(OWLClassExpression c, boolean sat, Double satTime, String reasonerName) {
		this.c = c;
		this.sat = sat;
		this.satTime = satTime;
		this.reasonerName = reasonerName;
	}
	
	
	public SATResult(OWLClassExpression c, Double satTime) {
		this.c = c;
		this.satTime = satTime;
	}
	
	
	/**
	 * Get concept
	 * @return Concept
	 */
	public OWLClassExpression getConcept() {
		return c;
	}
	
	
	/**
	 * Check if concept is satisfiable
	 * @return true if satisfiable, false otherwise
	 */
	public boolean isSatisfiable() {
		return sat;
	}
	
	
	/**
	 * Get SAT test time
	 * @return SAT test time (in seconds)
	 */
	public Double getSatTestTime() {
		return satTime;
	}
	
	
	/**
	 * Set elapsed time until getting a SAT result
	 * @param t	Time elapsed (in seconds)
	 */
	public void setElapsedTime(Double t) {
		elapsedTime = t;
	}
	
	
	/**
	 * Get elapsed time (in seconds)
	 * @return Elapsed time (in seconds) until getting a SAT result
	 */
	public Double getElapsedTime() {
		return elapsedTime;
	}
	
	
	/**
	 * Get reasoner used
	 * @return Reasoner used
	 */
	public String getReasonerUsed() {
		return reasonerName;
	}
}