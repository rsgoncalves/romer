package uk.ac.manchester.cs.romer.reasonertasks;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class RealizerResult extends EntailmentGeneratorResult {

	/**
	 * Constructor
	 * @param results	Reasoning task results
	 * @param time	Task time
	 * @param errorMsg	Error message (if applicable)
	 */
	public RealizerResult(Set<OWLAxiom> results, double time, String errorMsg, boolean isConsistent) {
		super(results, time, errorMsg, isConsistent);
	}
}
