package uk.ac.manchester.cs.romer.reasonertasks;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class EntailmentGeneratorResult {
	private Set<OWLAxiom> results;
	private double time;
	private String errorMsg;
	private boolean isConsistent, timedOut = false;
	
	/**
	 * Constructor
	 * @param results	Reasoning task results
	 * @param time	Task time
	 * @param errorMsg	Error message (if applicable)
	 * @param isConsistent	true if ontology is consistent
	 */
	public EntailmentGeneratorResult(Set<OWLAxiom> results, double time, String errorMsg, boolean isConsistent, boolean timedOut) {
		this.results = results;
		this.time = time;
		this.errorMsg = errorMsg;
		this.isConsistent = isConsistent;
		this.timedOut = timedOut;
	}
	
	
	/**
	 * Constructor 2
	 * @param results	Reasoning task results
	 * @param time	Task time
	 * @param errorMsg	Error message (if applicable)
	 * @param isConsistent	true if ontology is consistent
	 */
	public EntailmentGeneratorResult(Set<OWLAxiom> results, double time, String errorMsg, boolean isConsistent) {
		this.results = results;
		this.time = time;
		this.errorMsg = errorMsg;
		this.isConsistent = isConsistent;
	}
	
	
	/**
	 * Get the set of entailments
	 * @return Inferred axioms
	 */
	public Set<OWLAxiom> getEntailments() {
		return results;
	}
	
	
	/**
	 * Get the time elapsed executing the task 
	 * @return Reasoning task time
	 */
	public double getReasoningTaskTime() {
		return time;
	}
	
	
	/**
	 * Get the number of entailments
	 * @return Number of entailments
	 */
	public int getNumberOfEntailments() {
		return results.size();
	}
	
	
	/**
	 * Get error message, where one was returned by the reasoner
	 * @return Error message (if applicable)
	 */
	public String getErrorMessage() {
		return errorMsg;
	}
	
	
	/**
	 * Check if ontology is consistent
	 * @return true if ontology is consistent, false otherwise
	 */
	public boolean isConsistent() {
		return isConsistent;
	}
	
	
	/**
	 * Check if reasoning task timed out
	 * @return true if operation timed out, false otherwise
	 */
	public boolean timedOut() {
		return timedOut;
	}
}