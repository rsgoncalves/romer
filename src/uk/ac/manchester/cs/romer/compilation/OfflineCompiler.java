package uk.ac.manchester.cs.romer.compilation;

import java.io.IOException;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.manchester.cs.romer.approximation.Approximation;
import uk.ac.manchester.cs.romer.hotspots.HotspotFinder;
import uk.ac.manchester.cs.romer.hotspots.HotspotFinder.HotspotCandidateType;
import uk.ac.manchester.cs.romer.hotspots.HotspotFinder.HotspotIndicatorStrategy;
import uk.ac.manchester.cs.romer.hotspots.HotspotFinder.HotspotSearchStrategy;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class OfflineCompiler {
	private OWLOntology ont;
	private OWLOntologyManager man;
	private String ontPath, outputDir, reasonerName;
	private long classificationThreshold;
	private boolean verbose;
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 * @param ontPath	Ontology file path
	 * @param outputDir	Output directory
	 * @param reasonerName	Reasoner name
	 * @param classificationThreshold	Threshold for classification
	 * @param verbose	Verbose mode
	 */
	public OfflineCompiler(OWLOntology ont, String ontPath, String outputDir, String reasonerName, long classificationThreshold, boolean verbose) {
		this.ont = ont;
		this.ontPath = ontPath;
		this.outputDir = outputDir;
		this.reasonerName = reasonerName;
		this.classificationThreshold = classificationThreshold;
		this.verbose = verbose;
		this.man = ont.getOWLOntologyManager();
	}
	
	
	/**
	 * Compile and get the closest (most complete, where applicable) approximation
	 * @param strat	Hotspot search strategy
	 * @return Closest approximation
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Approximation compileAndGetClosestApproximation(HotspotSearchStrategy strat) throws OWLOntologyCreationException, IOException, InterruptedException {
		Set<Approximation> approxs = compileOntology(strat);
		if(approxs.size() == 1)
			return approxs.iterator().next();
		else {
			int maxEnts = 0;
			Approximation best = null;
			for(Approximation a : approxs) {
				Set<OWLAxiom> classHier = getClassHierarchy(a);
				if(classHier.size() > maxEnts)
					best = a;
				else if(classHier.size() == maxEnts && best != null) {
					if(getApproximationTime(a) < getApproximationTime(best))
						best = a;
				}
			}
			return best;
		}
	}
	
	
	/**
	 * Compile and serialize the closest (most complete, where applicable) approximation
	 * @param strat	Hotspot search strategy
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void compileAndSerializeClosestApproximation(HotspotSearchStrategy strat) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, InterruptedException {
		Approximation a = compileAndGetClosestApproximation(strat);
		serialize(getClassHierarchy(a), a.getApproximationTypeName());
	}
	
	
	/**
	 * Get class hierarchy of a given approximation
	 * @param a	Approximation
	 * @return Class hierarchy of approximation
	 */
	public Set<OWLAxiom> getClassHierarchy(Approximation a) {
		Set<OWLAxiom> entailments = a.getClassifierResults().getEntailments();
		entailments.addAll(a.getHotspot().getClassificationResults().getEntailments());
		return entailments;
	}
	
	
	/**
	 * Get approximation time, consisting of preparation + classification times 
	 * @param a	Approximation
	 * @return Time spent on preparing and classifying approximation
	 */
	public double getApproximationTime(Approximation a) {
		return a.getClassifierResults().getReasoningTaskTime() + a.getPreparationTime();
	}
	
	
	/**
	 * Compile ontology according to given hotspot search strategy
	 * @param strat	Hotspot search strategy
	 * @return Set of approximations
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Set<Approximation> compileOntology(HotspotSearchStrategy strat) throws OWLOntologyCreationException, IOException, InterruptedException {
		if(strat.equals(HotspotSearchStrategy.SEQ))
			return compileOntologySequentially();
		else if(strat.equals(HotspotSearchStrategy.CON))
			return compileOntologyConcurrently();
		else
			throw new Error("Unknown hotspot search strategy: Possible values are [ SEQ | CON ]");
	}
	
	
	/**
	 * Compile ontology in a concurrent manner
	 * @return Set of approximations
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Set<Approximation> compileOntologyConcurrently() throws OWLOntologyCreationException, IOException, InterruptedException {
		HotspotFinder hsf = new HotspotFinder(ont, reasonerName, ontPath, outputDir, classificationThreshold, HotspotIndicatorStrategy.SAT, HotspotSearchStrategy.CON, verbose);
		hsf.hotspotSizeThreshold = 0; // I.e., don't care about size
		return hsf.findApproximations(HotspotCandidateType.BOTMOD, false);
	}
	
	
	/**
	 * Compile ontology in a sequential manner
	 * @return Set of approximations
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Set<Approximation> compileOntologySequentially() throws OWLOntologyCreationException, IOException, InterruptedException {
		HotspotFinder hsf = new HotspotFinder(ont, reasonerName, ontPath, outputDir, classificationThreshold, HotspotIndicatorStrategy.SAT, HotspotSearchStrategy.SEQ, verbose);
		hsf.hotspotSizeThreshold = 0; // I.e., don't care about size
		return hsf.findApproximations(HotspotCandidateType.BOTMOD, false);
	}

	
	/**
	 * Serialize given set of axioms with desired filename
	 * @param axioms	Set of axioms
	 * @param filename	File name
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 */
	private void serialize(Set<OWLAxiom> axioms, String filename) throws OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntology out = man.createOntology(axioms);
		man.saveOntology(out, IRI.create("file:" + outputDir + filename + ".owl"));
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
