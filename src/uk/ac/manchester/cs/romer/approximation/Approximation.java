package uk.ac.manchester.cs.romer.approximation;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import uk.ac.manchester.cs.romer.approximation.ApproximationGenerator.ApproximationType;
import uk.ac.manchester.cs.romer.hotspots.Hotspot;
import uk.ac.manchester.cs.romer.reasonertasks.ClassifierResult;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class Approximation {
	private Set<OWLAxiom> approxAxioms;
	private Hotspot hotspot;
	private String approxFile, hotspotFile;
	private Double prepTime;
	private Integer size;
	private ClassifierResult clResult;
	private ApproximationType type;
	
	/**
	 * Constructor
	 * @param approxAxioms	Ontology approximation
	 * @param approxFile	Approximation file path
	 * @param hotspot	Hotspot
	 * @param hotspotFile	Hotspot file path
	 * @param prepTime	Approximation preparation time
	 * @param size	Size of approximation
	 * @param type	Type of approximation
	 */
	public Approximation(Set<OWLAxiom> approxAxioms, String approxFile, Hotspot hotspot, String hotspotFile, Double prepTime, Integer size, ApproximationType type) {
		this.approxAxioms = approxAxioms;
		this.approxFile = approxFile;
		this.hotspot = hotspot;
		this.hotspotFile = hotspotFile;
		this.prepTime = prepTime;
		this.size = size;
		this.type = type;
	}
	
	
	/**
	 * Set approximation classification results
	 * @param clResult	Classifier results
	 */
	public void attachClassifierResult(ClassifierResult clResult) {
		this.clResult = clResult;
	}
	
	
	/**
	 * Get classifier result
	 * @return Classification results
	 */
	public ClassifierResult getClassifierResults() {
		return clResult;
	}
	
	
	/**
	 * Get approximation
	 * @return Approximation
	 */
	public Set<OWLAxiom> getApproximation() {
		return approxAxioms;
	}
	
	
	/**
	 * Get hostpot
	 * @return Hotspot object
	 */
	public Hotspot getHotspot() {
		return hotspot;
	}
	
	
	/**
	 * Get absolute path to approximation file
	 * @return Absolute path to approximation file
	 */
	public String getApproximationFilePath() {
		return approxFile;
	}
	
	
	/**
	 * Get number of axioms in approximation
	 * @return Number of axioms in approximation
	 */
	public Integer getApproximationSize() {
		return size;
	}
	
	
	/**
	 * Get the approximation type
	 * @return Approximation type
	 */
	public ApproximationType getApproximationType() {
		return type;
	}
	
	
	/**
	 * Get the approximation type name
	 * @return Approximation type name
	 */
	public String getApproximationTypeName() {
		return type.name;
	}
	
	
	/**
	 * Get absolute path to hotspot file
	 * @return Absolute path to hotspot file
	 */
	public String getHotspotFilePath() {
		return hotspotFile;
	}
	
	
	/**
	 * Get approximation preparation time
	 * @return Approximation preparation time
	 */
	public Double getPreparationTime() {
		return prepTime;
	}
	
	
	/**
	 * Get signature of approximation
	 * @return Approximation signature
	 */
	public Set<OWLEntity> getSignature() {
		Set<OWLEntity> sig = new HashSet<OWLEntity>();
		for(OWLAxiom ax : getApproximation()) {
			sig.addAll(ax.getSignature());
		}
		return sig;
	}
	
	
	/**
	 * Get the union of remainder and hotspot entailments
	 * @return Union of remainder and hotspot entailments
	 */
	public Set<OWLAxiom> getRemainderAndHotspotEntailments() {
		Set<OWLAxiom> output = new HashSet<OWLAxiom>();
		if(hotspot.getClassificationResults() != null)
			output.addAll(hotspot.getClassificationResults().getEntailments());
		if(getClassifierResults() != null)
			output.addAll(getClassifierResults().getEntailments());
		return output;
	}
}
