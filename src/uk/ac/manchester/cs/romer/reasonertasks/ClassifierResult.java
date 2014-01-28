package uk.ac.manchester.cs.romer.reasonertasks;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ClassifierResult extends EntailmentGeneratorResult {
	private Set<OWLClass> unsatClasses;
	private int nrUnsatClasses;
	
	/**
	 * Constructor 1.1
	 * @param results	Classification results
	 * @param time	Classification time
	 * @param errorMsg	Error message (if applicable)
	 * @param isConsistent	true if ontology is consistent
	 * @param timedOut	true if classification timed out
	 */
	public ClassifierResult(Set<OWLAxiom> results, double time, Set<OWLClass> unsatClasses, String errorMsg, boolean isConsistent, boolean timedOut) {
		super(results, time, errorMsg, isConsistent, timedOut);
		this.unsatClasses = unsatClasses;
		this.nrUnsatClasses = unsatClasses.size();
	}
	
	
	/**
	 * Constructor 1.2
	 * @param results	Classification results
	 * @param time	Classification time
	 * @param errorMsg	Error message (if applicable)
	 * @param isConsistent	true if ontology is consistent
	 */
	public ClassifierResult(Set<OWLAxiom> results, double time, Set<OWLClass> unsatClasses, String errorMsg, boolean isConsistent) {
		super(results, time, errorMsg, isConsistent);
		this.unsatClasses = unsatClasses;
		this.nrUnsatClasses = unsatClasses.size();
	}
	
	
	/**
	 * Constructor 2.1
	 * @param results	Classification results
	 * @param time	Classification time
	 * @param errorMsg	Error message (if applicable)
	 * @param isConsistent	true if ontology is consistent
	 * @param timedOut	true if classification timed out
	 */
	public ClassifierResult(Set<OWLAxiom> results, double time, int nrUnsatClasses, String errorMsg, boolean isConsistent, boolean timedOut) {
		super(results, time, errorMsg, isConsistent, timedOut);
		this.nrUnsatClasses = nrUnsatClasses;
	}
	
	
	/**
	 * Constructor 2.2
	 * @param results	Classification results
	 * @param time	Classification time
	 * @param errorMsg	Error message (if applicable)
	 * @param isConsistent	true if ontology is consistent
	 */
	public ClassifierResult(Set<OWLAxiom> results, double time, int nrUnsatClasses, String errorMsg, boolean isConsistent) {
		super(results, time, errorMsg, isConsistent);
		this.nrUnsatClasses = nrUnsatClasses;
	}
	
	
	/**
	 * Get the set of unsatisfiable classes
	 * @return Set of unsatisfiable classes
	 */
	public Set<OWLClass> getUnsatisfiableClasses() {
		return unsatClasses;
	}
	
	
	/**
	 * Get the number of unsatisfiable classes
	 * @return Number of unsatisfiable classes
	 */
	public int getNumberOfUnsatisfiableClasses() {
		return nrUnsatClasses;
	}
}
