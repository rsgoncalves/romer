package uk.ac.manchester.cs.romer.reasonertasks;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.util.InferredClassAxiomGenerator;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class CustomInferredSubClassAxiomGenerator extends InferredClassAxiomGenerator<OWLSubClassOfAxiom> {
	private boolean directOnly;
	
	/**
	 * Constructor
	 * @param directOnly	true if asserted atomic subsumptions should be included, false otherwise 
	 */
	public CustomInferredSubClassAxiomGenerator(boolean directOnly) {
		this.directOnly = directOnly;
	}
	
	
	@Override
	protected void addAxioms(OWLClass entity, OWLReasoner reasoner, OWLDataFactory dataFactory, Set<OWLSubClassOfAxiom> result) throws TimeOutException {
		if(reasoner.isSatisfiable(entity)) {
			for(OWLClass sup : reasoner.getSuperClasses(entity, directOnly).getFlattened()) {
				result.add(dataFactory.getOWLSubClassOfAxiom(entity, sup));
			}
		} 
		else
			result.add(dataFactory.getOWLSubClassOfAxiom(entity, dataFactory.getOWLNothing()));
	}
	
	
	public String getLabel() {
        return "Subclasses";
    }
}
