package uk.ac.manchester.cs.romer.approximation;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class TrOWLELApproximation {	
	private OWLOntologyManager man;
	private OWLDataFactory df;
	private Set<OWLClass> newClasses;

	
	/**
	 * Constructor
	 */
	public TrOWLELApproximation() {
		man = OWLManager.createOWLOntologyManager();
		newClasses = new HashSet<OWLClass>();
		df = man.getOWLDataFactory();
	}

	
	/**
	 * Get EL approximation of given ontology
	 * @param ont	OWLOntology
	 * @return Approximation as an OWLOntology
	 */
	public OWLOntology getApproximatedOntology(OWLOntology ont) throws OWLOntologyCreationException {
		Set<OWLAxiom> approxAxioms = getApproximatedOntologyAxioms(ont);
		return man.createOntology(approxAxioms);
	}


	/**
	 * Get EL approximation of given ontology
	 * @param ont	OWLOntology
	 * @return Approximation as a set of axioms
	 */
	public Set<OWLAxiom> getApproximatedOntologyAxioms(OWLOntology ont) throws OWLOntologyCreationException {
		// Get subconcepts
		System.out.println("\nGetting sub-concepts...");
		Set<OWLClassExpression> scs_o1 = new HashSet<OWLClassExpression>();
		getSubConcepts(ont, scs_o1);
		System.out.println("...done. Nr. subconcepts: " + scs_o1.size());

		// Map each subconcept
		System.out.println("Mapping subconcepts...");
		Map<OWLClassExpression,OWLClass> map_o1 = mapSignature(ont, scs_o1);
		System.out.println("...done");

		newClasses.addAll(map_o1.values());

		// Get approximation
		System.out.println("Approximating ontology...");
		Set<OWLAxiom> approx = approximateToEL(ont, map_o1, scs_o1);
		System.out.println("...done\n Approximated ontology contains " + approx.size() + " axioms.");

		return approx;
	}


	/**
	 * Approximate an ontology to EL based on TrOWL's EL-reduction algorithm
	 * @param ont	OWLOntology
	 * @param map	Map of expressions and their corresponding created classes
	 * @param scs	Set of subconcepts in the ontology
	 * @return Approximation as a set of axioms
	 */
	private Set<OWLAxiom> approximateToEL(OWLOntology ont, Map<OWLClassExpression,OWLClass> map, Set<OWLClassExpression> scs) {
		Set<OWLAxiom> output = new HashSet<OWLAxiom>();

		// Step 1
		for(OWLAxiom ax : ont.getLogicalAxioms()) {
			if(ax.isOfType(AxiomType.SUBCLASS_OF)) {
				OWLSubClassOfAxiom subcAx = (OWLSubClassOfAxiom) ax;
				OWLClassExpression subc = subcAx.getSubClass();
				OWLClassExpression supc = subcAx.getSuperClass();
				if(!supc.isTopEntity()) {
					OWLAxiom newAx = df.getOWLSubClassOfAxiom(getMapping(subc, map), getMapping(supc, map));
					output.add(newAx);
				}
			}
			else if(ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
				OWLEquivalentClassesAxiom equivAc = (OWLEquivalentClassesAxiom) ax;
				Object[] subAxs = equivAc.asOWLSubClassOfAxioms().toArray();

				OWLSubClassOfAxiom subcAx = (OWLSubClassOfAxiom) subAxs[0];
				OWLClassExpression subc = subcAx.getSubClass();
				OWLClassExpression supc = subcAx.getSuperClass();

				OWLAxiom newAx = df.getOWLEquivalentClassesAxiom(getMapping(subc, map), getMapping(supc, map));
				output.add(newAx);
			}
		}

		// Step 2
		for(OWLClassExpression ce : scs) {
			if(ce.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) {
				OWLObjectIntersectionOf conj = (OWLObjectIntersectionOf) ce;
				Set<OWLClassExpression> conjuncts = new HashSet<OWLClassExpression>();

				for(OWLClassExpression conjunct : conj.asConjunctSet()) {
					conjuncts.add(getMapping(conjunct, map));
				}

				OWLObjectIntersectionOf newConj = df.getOWLObjectIntersectionOf(conjuncts);

				OWLAxiom newAx = df.getOWLEquivalentClassesAxiom(getMapping(ce, map), newConj);
				output.add(newAx);
			}
			else if(ce.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
				OWLObjectSomeValuesFrom exists = (OWLObjectSomeValuesFrom) ce;
				OWLClassExpression filler = exists.getFiller();
				OWLObjectPropertyExpression prop = exists.getProperty();

				//				System.out.println("Filler: " + filler);
				//				System.out.println("Prop: " + prop);

				if(getMapping(filler, map) != null) {
					OWLObjectSomeValuesFrom rest = df.getOWLObjectSomeValuesFrom(prop, getMapping(filler, map));

					OWLAxiom newAx = df.getOWLEquivalentClassesAxiom(getMapping(ce, map), rest);
					output.add(newAx);
				}
			}
			else {
				OWLAxiom newAx = df.getOWLSubClassOfAxiom(getMapping(ce, map), df.getOWLThing());
				output.add(newAx);
			}
		}

		return output;
	}


	/**
	 * Map subconcepts to fresh concept names
	 * @param ont	OWLOntology
	 * @param scs	Set of subconcepts in the ontology
	 * @return Mapping of subconcepts to newly created concept names 
	 */
	private Map<OWLClassExpression,OWLClass> mapSignature(OWLOntology ont, Set<OWLClassExpression> scs) {
		Map<OWLClassExpression,OWLClass> map = new HashMap<OWLClassExpression,OWLClass>();  

		Set<OWLClassExpression> cesToAdd = new HashSet<OWLClassExpression>();

		int counter = 0;
		for(OWLClassExpression ce : scs) {
			if(!ce.isAnonymous()) {
				map.put(ce, ce.asOWLClass());

				OWLClass c = df.getOWLClass(IRI.create("sc_" + counter+1));
				OWLClassExpression ce2 = ce.getComplementNNF();

				cesToAdd.add(ce2);
				map.put(ce2, c);

				counter += 2;
			}
			else {
				OWLClass c = df.getOWLClass(IRI.create("sc_" + counter));
				map.put(ce, c);

				OWLClass nC = df.getOWLClass(IRI.create("sc_" + counter+1));
				OWLClassExpression ce2 = ce.getComplementNNF();

				cesToAdd.add(ce2);
				map.put(ce2, nC);

				counter += 2;
			}
			//				else {
			//					map.put(ce, map_o1.get(ce));
			//				}
		}

		scs.addAll(cesToAdd);

		return map;
	}


	/**
	 * Get subconcepts of an ontology
	 * @param ont	OWLOntology
	 * @param sc	Set of subconcepts
	 * @return Set of subconcepts in the ontology
	 */
	private Set<OWLClassExpression> getSubConcepts(OWLOntology ont, Set<OWLClassExpression> sc) {
		for(OWLAxiom ax : ont.getLogicalAxioms()) {
			Set<OWLClassExpression> ax_sc = ax.getNestedClassExpressions();
			for(OWLClassExpression ce : ax_sc) {
				if(!sc.contains(ce) && !ce.isOWLThing() && !ce.isOWLNothing()) {
					sc.add(ce);
					getSubConcepts(ce, sc);
				}
				else
					getSubConcepts(ce, sc);
			}
		}
		return sc;
	}


	/**
	 * Recursively get sub-concepts of a sub-concept
	 * @param ce	Concept to check
	 * @param sc	Set of subconcepts
	 */
	private void getSubConcepts(OWLClassExpression ce, Set<OWLClassExpression> sc) {
		if(ce.getNestedClassExpressions().size() > 1) {
			for(OWLClassExpression c : ce.getNestedClassExpressions()) {
				if(!sc.contains(c) && !c.isOWLThing() && !c.isOWLNothing()) {
					sc.add(c);
					getSubConcepts(c, sc);
				}
			}
		}
		else {
			if(!sc.contains(ce) && !ce.isOWLThing() && !ce.isOWLNothing()) {
				sc.add(ce);
				getSubConcepts(ce, sc);
			}
		}
	}

	
	/**
	 * Get the mapping of a particular subconcept 
	 * @param ce	Concept
	 * @param map	Map of subconcepts to classes
	 * @return Concept name
	 */
	private OWLClassExpression getMapping(OWLClassExpression ce, Map<OWLClassExpression, OWLClass> map) {
		return map.get(ce);
	}


	/**
	 * Get the set of new class names introduced by the reduction algorithm
	 * @return Set of OWLClasses created
	 */
	public Set<OWLClass> getNewClassNames() {
		return newClasses;
	}


	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException {
		File f1 = new File(args[0]);
		System.out.println("Loading ontology...");

		//			String o1_path = f1.getPath();
		//			o1_path = o1_path.substring(0, o1_path.lastIndexOf("/")+1);
		//			System.out.println("Output approximated ontology to: " + o1_path);

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();

		OWLOntology ont1 = man.loadOntologyFromOntologyDocument(new IRIDocumentSource(IRI.create("file:" + f1.getPath())));

		System.out.println("...done");
		System.out.println("Ontology has " + ont1.getLogicalAxiomCount() + " logical axioms");

		TrOWLELApproximation app = new TrOWLELApproximation();
		app.getApproximatedOntology(ont1);
	}
}