package uk.ac.manchester.cs.romer.reasonertasks;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.semanticweb.HermiT.datatypes.UnsupportedDatatypeException;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;
import org.semanticweb.owlapi.reasoner.ReasonerInternalException;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;

import uk.ac.manchester.cs.romer.OntologyLoader;
import uk.ac.manchester.cs.romer.ReasonerLoader;
import uk.ac.manchester.cs.romer.utils.CSVSerializer;
import uk.ac.manchester.cs.romer.utils.ResultsSerializer;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class EntailmentGenerator {	
	private OWLOntology ont;
	private String reasonerName;
	private ThreadMXBean bean;
	private CSVSerializer s;
	private ResultsSerializer rs;
	private boolean verbose, includeAsserted, directOnly;

	
	/**
	 * Constructor
	 * @param ont	OWLOntology
	 * @param reasonerName	Reasoner name
	 * @param verbose	Verbose mode
	 */
	public EntailmentGenerator(OWLOntology ont, String reasonerName, boolean includeAsserted, boolean directOnly, boolean ignoreAbox, boolean verbose) {
		this.ont = ont;
		this.includeAsserted = includeAsserted;
		this.directOnly = directOnly;
		this.verbose = verbose;
		this.reasonerName = reasonerName;
		this.bean = ManagementFactory.getThreadMXBean();
		if(ignoreAbox) System.out.println("\tIgnoring Abox axioms");
	}
		
	
	/**
	 * Alternative constructor used for benchmarking: records ontology load time, reasoner creation, etc
	 * @param ontFile	Ontology file
	 * @param reasonerName	Reasoner name
	 * @param outputDir	Output directory for log and/or reasoning task results
	 * @param verbose	Verbose mode
	 */
	public EntailmentGenerator(File ontFile, String reasonerName, String outputDir, boolean includeAsserted,
			boolean directOnly, boolean ignoreAbox, boolean verbose) {
		this.includeAsserted = includeAsserted;
		this.directOnly = directOnly;
		this.verbose = verbose;
		this.reasonerName = reasonerName;
		
		String ontDir = ontFile.getAbsolutePath();
		ontDir = ontDir.substring(0, ontDir.lastIndexOf("/"));
		
		this.s = new CSVSerializer(outputDir, reasonerName, true);
		this.ont = loadOntology(ontFile, ontFile.getName(), s, ignoreAbox, verbose);
		this.bean = ManagementFactory.getThreadMXBean();
		rs = new ResultsSerializer(ontDir, ontFile.getName());
		if(ignoreAbox) System.out.println("\tIgnoring Abox axioms");
	}

	
	/**
	 * Create reasoner and record its creation time
	 * @param reasonerName	Name of reasoner to be used
	 * @param verbose	Verbose mode
	 * @return OWLReasoner
	 */
	private OWLReasoner createReasoner() {
		ReasonerLoader reasonerCreator = new ReasonerLoader(reasonerName, ont, verbose);
		return reasonerCreator.getReasoner();
	}
	
	
	/**
	 * Load ontology and record its load time
	 * @param ontFile	Ontology file
	 * @param verbose	Verbose mode
	 * @return OWLOntology
	 * @throws IOException 
	 */
	private OWLOntology loadOntology(File ontFile, String ontName, CSVSerializer s, boolean ignoreAbox, boolean verbose) {
		OntologyLoader ontLoader = new OntologyLoader(ontFile, verbose);
		OWLOntology ont = null;
		try {
			ont = ontLoader.loadOntology(ignoreAbox);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		double parseTime = ontLoader.getLoadTime();
		s.setStandardRow(ont, ontFile.getName(), parseTime);
		return ont;
	}
	
	
	/**
	 * Classify ontology
	 * @return Classification results
	 * @throws IOException 
	 */
	public ClassifierResult classify() {
		Set<OWLAxiom> results = new HashSet<OWLAxiom>();
		Set<OWLClass> unsat = new HashSet<OWLClass>();
		String errorMsg = "";
		boolean isConsistent = true;
		long start = bean.getCurrentThreadCpuTime(); 
		double creationTime = 0;	
		try {
			OWLReasoner reasoner = createReasoner();
			creationTime = (bean.getCurrentThreadCpuTime()-start)/1000000000.0;
			System.out.println("\tReasoner creation time: " + creationTime + " seconds");
			
			if(reasoner != null) {
				if(verbose) System.out.print(" Classifying ontology... ");
				start = bean.getCurrentThreadCpuTime();
				if(reasonerName.equalsIgnoreCase("elk")) reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

				CustomInferredSubClassAxiomGenerator subs = new CustomInferredSubClassAxiomGenerator(directOnly);
				results.addAll(subs.createAxioms(ont.getOWLOntologyManager(), reasoner));
			}
		}
		catch(InconsistentOntologyException e) {
			isConsistent = false;
			System.out.println("\n\tInconsistent ontology");
		} catch(UnsupportedDatatypeException e) {
			String datatype = e.getMessage();
			datatype = datatype.substring(datatype.indexOf("'")+1, datatype.lastIndexOf("'"));
			errorMsg = "UnsupportedDatatype: " + datatype;
			
			System.out.println("\n\tERROR\t" + errorMsg);
			e.printStackTrace();
		} catch(OutOfMemoryError e) {
			errorMsg = "OutOfMemoryError: " + e.getMessage();
			System.out.println("\n\tERROR\t" + errorMsg);
		} catch(StackOverflowError e) {
			errorMsg = "StackOverflowError: " + e.getMessage();
			System.out.println("\n\tERROR\t" + errorMsg);
//			e.printStackTrace();
		} catch(ReasonerInternalException e) {
			String msg = e.getMessage();
			if(msg.contains("Unsupported datatype")) {
				String datatype = "";
				if(reasonerName.equalsIgnoreCase("fact"))
					datatype = msg.substring(msg.indexOf("'")+1, msg.lastIndexOf("'"));
				else
					datatype = msg.replace("Unsupported datatype ", "");
					
				errorMsg = "UnsupportedDatatype: " + datatype;
			}
			else errorMsg = "ReasonerInternalException: " + msg;
			
			System.out.println("\n\tERROR\t" + errorMsg);
			e.printStackTrace();
		} catch(OWLReasonerRuntimeException e) {
			errorMsg = "OWLReasonerRuntimeException: " + e.getMessage();
			System.out.println("\n\tERROR\t" + errorMsg);
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			errorMsg = "IllegalArgumentException: " + e.getMessage();
			System.out.println("\n\tERROR\t" + errorMsg);
			e.printStackTrace();
		}
		
		long end = bean.getCurrentThreadCpuTime();
		double total = (end-start)/1000000000.0;
		
		if(verbose && errorMsg.equals("")) System.out.println(" done\n\tClassification time: " + total + " seconds");
		else if(verbose) System.out.println(" done\n\tElapsed time: " + total + " seconds"); 
		
		if(s != null) {
			if(creationTime == 0) s.appendToCsv("Reasoner Creation Time", total + "");
			else s.appendToCsv("Reasoner Creation Time", creationTime + "");
		}
		
		if(!isConsistent) {
			unsat = ont.getClassesInSignature();
			if(verbose) System.out.println("Ontology is inconsistent");
		}
		
		if(!results.isEmpty()) {
			results = pruneClassificationResults(results);
			unsat = getUnsatisfiableClasses(results);
		}
		
		if(verbose && isConsistent) System.out.println("\tNr. Entailments: " + results.size());
		if(verbose) System.out.println("\tNr. Unsatisfiable Classes: " + unsat.size());
		
		errorMsg = errorMsg.replaceAll(",", ";");
		
		return new ClassifierResult(results, total, unsat, errorMsg, isConsistent);
	}
	
	
	/**
	 * Classify ontology with a timeout
	 * @param timeout	Timeout for classification (in milliseconds)
	 * @return Classification results
	 */	
	public ClassifierResult classify(long timeout) {
		if(timeout != 0) {
			Timer t = new Timer(true);
			t.schedule(interrupt, timeout);
		}
		return classify();
	}
	
	
	/**
	 * Given a set of entailments, retrieve those unsatisfiable classes
	 * @param entailments	Set of entailments
	 * @return Set of unsatisfiable classes
	 */
	private Set<OWLClass> getUnsatisfiableClasses(Set<OWLAxiom> entailments) {
		Set<OWLClass> unsat = new HashSet<OWLClass>();
		for(OWLAxiom ax : entailments) {
			if(ax.isOfType(AxiomType.SUBCLASS_OF)) {
				OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom)ax;
				if(subAx.getSuperClass().isOWLNothing()) {
					OWLClassExpression sub = subAx.getSubClass();
					if(!sub.isAnonymous())
						unsat.add(sub.asOWLClass());
				}
			}
		}
		return unsat;
	}
	
	
	/**
	 * Realize ontology
	 * @return Realization results
	 */
	public RealizerResult realize() {
		if(verbose) System.out.print(" Realizing ontology... ");
		Set<OWLAxiom> results = null;
		String errorMsg = "";
		boolean isConsistent = true;
		
		long start = 0;
		try {
			OWLReasoner reasoner = createReasoner();
			if(reasonerName.equalsIgnoreCase("elk")) reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			
			start = bean.getCurrentThreadCpuTime();
			InferredClassAssertionAxiomGenerator gen = new InferredClassAssertionAxiomGenerator();
			results = new HashSet<OWLAxiom>(gen.createAxioms(ont.getOWLOntologyManager(), reasoner));
			
//			InferredPropertyAssertionGenerator pGen = new InferredPropertyAssertionGenerator();
//			results.addAll(pGen.createAxioms(ont.getOWLOntologyManager(), reasoner));
		}
		catch(InconsistentOntologyException e) {
			isConsistent = false;
			errorMsg = "Inconsistent";
		} catch(UnsupportedDatatypeException e) {
			errorMsg = "UnsupportedDatatype";
			e.printStackTrace();
		} catch(OutOfMemoryError e) {
			errorMsg = "OutOfMemoryError";
		} catch(StackOverflowError e) {
			errorMsg = "StackOverflowError";
			e.printStackTrace();
		} catch(ReasonerInternalException e) {
			errorMsg = e.getCause().toString();
			e.printStackTrace();
		}
		
		long end = bean.getCurrentThreadCpuTime();
		double total = (end-start)/1000000000.0;
		
		if(results != null) {
			if(!results.isEmpty())
				results = pruneRealizationAssertions(results);
			if(verbose) System.out.println("done\n\tRealization time: " + total + " seconds\n\tNr. Entailments: " + results.size());
		}
		
		return new RealizerResult(results, total, errorMsg, isConsistent);
	}
	
	
	/**
	 * Realize ontology with a timeout
	 * @return Realization results
	 */	
	public RealizerResult realize(long timeout) {
		Timer t = new Timer(true);
		t.schedule(interrupt, timeout);
		return realize();
	}
	
	
	/**
	 * Remove superfluous ({a} => Top) and asserted axioms
	 * @param axioms	Set of inferred axioms to remove asserted ones from
	 * @return Pruned set of inferences
	 */
	private Set<OWLAxiom> pruneRealizationAssertions(Set<OWLAxiom> axioms) {
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		for(OWLAxiom ax : axioms) {
			if(ax instanceof OWLClassAssertionAxiom) {
				OWLSubClassOfAxiom subAx = ((OWLClassAssertionAxiom)ax).asOWLSubClassOfAxiom();
				if(subAx.getSuperClass().isOWLThing() || subAx.getSubClass().isOWLNothing() || (!includeAsserted && ont.containsAxiom(ax)))
					toRemove.add(ax);
			}
			else if(!includeAsserted && ont.containsAxiom(ax))
				toRemove.add(ax);
		}
		axioms.removeAll(toRemove);
		return axioms;
	}
	
	
	/**
	 * Remove superfluous (A => Top, Bot => B) and asserted axioms
	 * @param axioms	Set of inferred axioms to remove asserted ones from
	 * @return Pruned set of inferences
	 */
	private Set<OWLAxiom> pruneClassificationResults(Set<OWLAxiom> axioms) {
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		for(OWLAxiom ax : axioms) {
			if(ax instanceof OWLSubClassOfAxiom) {
				OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom)ax; 
				if(subAx.getSuperClass().isOWLThing() || subAx.getSubClass().isOWLNothing() || (!includeAsserted && ont.containsAxiom(ax)))
					toRemove.add(ax);
			}
			else if(ax instanceof OWLSubObjectPropertyOfAxiom) {
				OWLSubObjectPropertyOfAxiom subAx = (OWLSubObjectPropertyOfAxiom)ax;
				if(subAx.getSuperProperty().isOWLTopObjectProperty() || subAx.getSubProperty().isOWLBottomObjectProperty() || 
						(!includeAsserted && ont.containsAxiom(ax)))
					toRemove.add(ax);
			}
			else if(ax instanceof OWLSubDataPropertyOfAxiom) {
				OWLSubDataPropertyOfAxiom subAx = (OWLSubDataPropertyOfAxiom)ax;
				if(subAx.getSuperProperty().isOWLTopDataProperty() || subAx.getSubProperty().isOWLBottomDataProperty() ||
						(!includeAsserted && ont.containsAxiom(ax)))
					toRemove.add(ax);
			}
			else if(!includeAsserted && ont.containsAxiom(ax))
				toRemove.add(ax);
		}
		axioms.removeAll(toRemove);
		return axioms;
	}
	
	
	/**
	 * Benchmark classification on the given ontology-reasoner pair
	 * @param timeout	Timeout in milliseconds
	 * @throws IOException
	 */
	public void benchmarkAndSerializeClassificationResults(long timeout) throws IOException {
		ClassifierResult r = null;
		if(timeout != 0) r = classify(timeout);
		else r = classify();
		
		if(r != null) {
			if(r.getErrorMessage() == "") {
				s.appendToCsv("Classification Time", r.getReasoningTaskTime() + "");
				
				if(r.isConsistent()) s.appendToCsv("Nr. Entailments", r.getNumberOfEntailments() + "");
				else s.appendToCsv("Nr. Entailments", "Inconsistent");

				s.appendToCsv("Nr. Unsat Classes", r.getNumberOfUnsatisfiableClasses() + "");
			}
			else {
				s.appendToCsv("Classification Time"," ");
				s.appendToCsv("Nr. Entailments"," ");
				s.appendToCsv("Nr. Unsat Classes"," ");
			}
			s.appendToCsv("Error", r.getErrorMessage());
		}
		s.finalize();

		if(r.getEntailments() != null && !r.getEntailments().isEmpty()) {
			String outFileName = rs.serialize(r);
			if(verbose) System.out.println("Classification results saved to: " + outFileName);
		}
		
		System.out.println("Done benchmarking");
		System.exit(0);
	}
	
	
	/**
	 * Benchmark realization on the given ontology-reasoner pair
	 * @param timeout	Timeout in milliseconds
	 * @throws IOException
	 */
	public void benchmarkAndSerializeRealizationResults(long timeout) throws IOException {
		RealizerResult r = null;
		if(timeout != 0)
			r = realize(timeout);
		else
			r = realize();

		s.appendToCsv("Realization Time", r.getReasoningTaskTime() + "");
		
		if(r.isConsistent()) s.appendToCsv("Nr. Entailments", r.getNumberOfEntailments() + "");
		else s.appendToCsv("Nr. Entailments", "Inconsistent");
		
		s.appendToCsv("Error", r.getErrorMessage());
		s.finalize();
		
		if(r.getEntailments() != null && !r.getEntailments().isEmpty())
			rs.serialize(r);
	}
	
	
	/**
	 * Interrupt trigger	
	 */
	private TimerTask interrupt = new TimerTask() {
		@Override
		public void run() {
			if(s != null) {
				s.appendToCsv("Reasoning Time", "timeout");
				try {s.finalize();} catch (IOException e) {e.printStackTrace();}
			}
			System.out.println("\n	Aborted: Reasoning task exceeded timeout");
			System.exit(0);
		}
	};
	

	/**
	 * main
	 * @throws OWLOntologyCreationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, IOException {
		System.out.println("Executing Entailment Generator...");
		String ontFile = null, reasonerName = null, outputDir = null;
		boolean verbose = false, classification = false, realization = false, includeAsserted = true, directOnly = false, ignoreAbox = false;
		long timeout = 0;
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-ont"))		ontFile = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			if(arg.equalsIgnoreCase("-cl"))			classification = true;
			if(arg.equalsIgnoreCase("-rl"))			realization = true;
			if(arg.equalsIgnoreCase("-t"))			timeout = Long.parseLong(args[++i].trim());
			if(arg.equalsIgnoreCase("-o"))			outputDir = args[++i].trim();
			if(arg.equalsIgnoreCase("-x"))			includeAsserted = false;
			if(arg.equalsIgnoreCase("-d"))			directOnly = true;
			if(arg.equalsIgnoreCase("-v"))			verbose = true;
			if(arg.equalsIgnoreCase("-b"))			ignoreAbox = true;
		}
		
		if(ontFile != null && reasonerName != null) {
			if(outputDir != null) {
				EntailmentGenerator gen = new EntailmentGenerator(new File(ontFile), reasonerName, outputDir, 
						includeAsserted, directOnly, ignoreAbox, verbose);
				if(classification)	gen.benchmarkAndSerializeClassificationResults(timeout);
				if(realization)		gen.benchmarkAndSerializeRealizationResults(timeout);
			}
			else {
				EntailmentGenerator gen = new EntailmentGenerator(new OntologyLoader(new File(ontFile), verbose).loadOntology(ignoreAbox), 
						reasonerName, includeAsserted, directOnly, ignoreAbox, verbose);
				if(classification)	gen.classify(timeout);
				if(realization)		gen.realize(timeout);
			}
		}
		else {
			throw new RuntimeException("Error: Minimum parameters are: -ont ONTOLOGY -reasoner REASONERNAME [ -cl | -rl ].\n" +
					"\tPlease review the usage information via the -h flag.");
		}
	}
}