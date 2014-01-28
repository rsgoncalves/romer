package uk.ac.manchester.cs.romer.approximation;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import uk.ac.manchester.cs.romer.approximation.ELApproximation.ELApproximationType;
import uk.ac.manchester.cs.romer.hotspots.Hotspot;
import uk.ac.manchester.cs.romer.reasonertasks.ClassifierResult;
import uk.ac.manchester.cs.romer.reasonertasks.EntailmentGenerator;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ApproximationGenerator {
	private ELApproximationType elApproxType;
	private ClassifierResult naiveApproxResults;
	private Hotspot hotspot;
	private OWLOntology ont;
	
	/**
	 * Constructor
	 * @param hotspot	Hot spot axioms
	 * @param ont	OWLOntology
	 */
	public ApproximationGenerator(Hotspot hotspot, OWLOntology ont) {
		this.hotspot = hotspot;
		this.ont = ont;
	}
	
	
	/**
	 * Constructor 2
	 * @param hotspot	Hot spot axioms
	 * @param ont	OWLOntology
	 * @param elApproxType	EL approximation type
	 */
	public ApproximationGenerator(Hotspot hotspot, OWLOntology ont, ELApproximationType elApproxType) {
		this.elApproxType = elApproxType;
		this.hotspot = hotspot;
		this.ont = ont;
	}


	/**
	 * Get approximation based on the given hot spot
	 * @param type	Approximation type
	 * @param reasonerName	Name of reasoner
	 * @return	Approximation as an OWLOntology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology getApproximationAsOntology(ApproximationType type, String reasonerName) throws OWLOntologyCreationException {
		Set<OWLAxiom> approx = getApproximationAxioms(type, reasonerName);
		return ont.getOWLOntologyManager().createOntology(approx);
	}


	/**
	 * Get approximation based on the given hot spot
	 * @param type	Approximation type
	 * @param reasonerName	Name of reasoner
	 * @return	Approximation as a set of axioms
	 */
	public Set<OWLAxiom> getApproximationAxioms(ApproximationType type, String reasonerName) {
		Set<OWLAxiom> result = null;
		if(type == ApproximationType.NAIVE)
			result = getRemainder();
		else if(type == ApproximationType.COMBINEDCL)
			result = getClosureCombination(reasonerName);
		else if(type == ApproximationType.REMCLM)
			result = getRemainderPlusClM(reasonerName);
		else if(type == ApproximationType.REMCLMELM)
			result = getRemainderPlusClMPlusELM(reasonerName);
		else if(type == ApproximationType.COMPLETE)
			result = getBottomModuleRemainder();
		return result;
	}


	/**
	 * Get a naive approximation where the hotspot is removed from the ontology
	 * @return Approximation as a set of axioms
	 */
	public Set<OWLAxiom> getRemainder() {
		Set<OWLAxiom> output = new HashSet<OWLAxiom>();
		output.addAll(ont.getLogicalAxioms());
		output.removeAll(hotspot.getAxioms());
		return output;
	}


	/**
	 * Get an approximation consisting of the combination of the atomic closures of the remainder and the hot spot
	 * @param reasonerName	Name of reasoner
	 * @return Approximation as a set of axioms
	 */
	public Set<OWLAxiom> getClosureCombination(String reasonerName) {
		Set<OWLAxiom> output = new HashSet<OWLAxiom>();
		Set<OWLAxiom> hsEnts = hotspot.getClassificationResults().getEntailments();
		if(hsEnts != null)
			output.addAll(hsEnts);
		else {
			ClassifierResult r = classify(hotspot.getHotspotAsOntology(), reasonerName);
			output.addAll(r.getEntailments());
			hotspot.attachClassifierResults(r);
		}
		
		if(naiveApproxResults == null) {
			try {
				naiveApproxResults = classify(getApproximationAsOntology(ApproximationType.NAIVE, reasonerName), reasonerName);
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			}
		}
		output.addAll(naiveApproxResults.getEntailments());
		return output;
	}


	/**
	 * Get an approximation consisting of axioms in the remainder, plus atomic closure of the hot spot
	 * @param reasonerName	Name of reasoner
	 * @return Approximation as a set of axioms
	 */
	public Set<OWLAxiom> getRemainderPlusClM(String reasonerName) {
		ClassifierResult hotspotClResult = hotspot.getClassificationResults();
		if(hotspotClResult == null) {
			hotspotClResult = classify(hotspot.getHotspotAsOntology(), reasonerName);
			hotspot.attachClassifierResults(hotspotClResult);
		}
		
		Set<OWLAxiom> result = new HashSet<OWLAxiom>(getRemainder());
		result.addAll(hotspotClResult.getEntailments());
		return result;
	}

	
	/**
	 * Get an approximation consisting of axioms in the remainder, plus atomic closure of the hot spot,
	 * plus an EL approximated version of the hot spot: set via setELApproximationType(). If no EL reduction
	 * algorithm is set, the default is a naive EL approximation
	 * @param reasonerName	Name of reasoner
	 * @return Approximation as a set of axioms
	 */
	public Set<OWLAxiom> getRemainderPlusClMPlusELM(String reasonerName) {
		Set<OWLAxiom> result = getRemainderPlusClM(reasonerName);
		ELApproximation elApprox = new ELApproximation(hotspot.getHotspotAsOntology());
		if(elApproxType != null)
			result.addAll(elApprox.getELApproximation(elApproxType));
		else
			result.addAll(elApprox.getELApproximation(ELApproximationType.NAIVE)); // Default to naive approximation
		return result;
	}
	
	
	/**
	 * Get an approximation which is a bottom locality module w.r.t. the converse signature of the hotspot,
	 * which should also be a bottom module in this approach, so that the union of the class hierarchies of
	 * both modules is equal to the class hierarchy of the original ontology
	 * @return Approximation as set of axioms
	 */
	public Set<OWLAxiom> getBottomModuleRemainder() {
		Set<OWLEntity> sig = ont.getSignature();
		sig.removeAll(hotspot.getSignature());
		return new SyntacticLocalityModuleExtractor(ont.getOWLOntologyManager(), ont, ModuleType.BOT).extract(sig);
	}
	
	
	/**
	 * Classify a given ontology
	 * @param ont	OWL ontology
	 * @param reasonerName	Reasoner name
	 * @return Set of entailments
	 */
	private ClassifierResult classify(OWLOntology ont, String reasonerName) {
		EntailmentGenerator gen = new EntailmentGenerator(ont, reasonerName, true, false, true, false);
		return gen.classify();
	}
	
	
	/**
	 * Set the classifier result for naive approximation 
	 * @param r	Classifier result for naive approximation
	 */
	public void setNaiveApproxClassifierResult(ClassifierResult r) {
		naiveApproxResults = r;
	}
	
	
	/**
	 * Set the desired EL approximation type for hotspot
	 * @param type	EL approximation type
	 */
	public void setELApproximationType(ELApproximationType type) {
		this.elApproxType = type;
	}

	
	/**
	 * Approximation type
	 */
	public enum ApproximationType {
		NAIVE ("Naive"), 					// Naive approximation: O\\M
		COMBINEDCL ("CombinedCls"), 		// Combined atomic closures of remainder and hot spot: Cl(O\\M) U Cl(M)
		REMCLM	("RemPlusClM"),				// Combine the remainder with the closure of the hot spot: O\\M U Cl(M)
		REMCLMELM ("RemPlusClMPlusELM"),	// Combine the remainder with the closure of the hot spot,and an EL approximation of the hotspot: O\\M U Cl(M) U EL(M)
		COMPLETE ("BotModule");				// Where the remainder is a bottom-module for the hotspot's converse signature

		String name;
		ApproximationType(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}
}