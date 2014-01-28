package uk.ac.manchester.cs.romer.hotspots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import uk.ac.manchester.cs.romer.Dispatcher;
import uk.ac.manchester.cs.romer.OntologyLoader;
import uk.ac.manchester.cs.romer.approximation.Approximation;
import uk.ac.manchester.cs.romer.approximation.ApproximationGenerator;
import uk.ac.manchester.cs.romer.approximation.ApproximationGenerator.ApproximationType;
import uk.ac.manchester.cs.romer.reasonertasks.ClassifierResult;
import uk.ac.manchester.cs.romer.reasonertasks.EntailmentGenerator;
import uk.ac.manchester.cs.romer.reasonertasks.SATOntologyTester;
import uk.ac.manchester.cs.romer.utils.ResultsSerializer;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class HotspotFinder {
	public int hotspotSizeThreshold, maxTests = 1000, minHotspots = 1;
	public double csatTimeout = 0;
	public ThreadMXBean bean;
	private final int MAX_PROCESSES = Runtime.getRuntime().availableProcessors();
	private SyntacticLocalityModuleExtractor starModExtractor, botModExtractor;
	private OWLOntology ont;
	private OWLDataFactory df;
	private OWLOntologyManager man;
	private HotspotIndicatorStrategy indStrategy;
	private HotspotSearchStrategy searchStrategy;
	private String ontPath, outputDir, reasonerName, log;
	private LinkedHashMap<Approximation,String> tempLog;
	private HashMap<OWLClass,Double> satTimeMap;
	private HashMap<String,Double> timings;
	private ClassifierResult ontResults;
	private volatile int candidateNr = 1, badSizeRems = 0, badSizeHotspots = 0;
	private long classificationThreshold;
	private boolean verbose;
	
	/**
	 * Constructor
	 * @param ont	OWLOntology
	 * @param reasonerName	Name of reasoner to be used
	 * @param ontPath	Ontology file path
	 * @param classificationThreshold	Maximum time (in milliseconds) for classification of the approximation
	 * @param indStrategy	Hot spot search strategy
	 */
	public HotspotFinder(OWLOntology ont, String reasonerName, String ontPath, String outputDir, long classificationThreshold,
			 HotspotIndicatorStrategy indStrategy, HotspotSearchStrategy searchStrategy, boolean verbose) {
		this.ont = ont;
		this.reasonerName = reasonerName;
		this.ontPath = ontPath;
		this.outputDir = outputDir;
		this.classificationThreshold = classificationThreshold;
		this.indStrategy = indStrategy;
		this.searchStrategy = searchStrategy;
		this.verbose = verbose;
		this.man = ont.getOWLOntologyManager();
		this.df = man.getOWLDataFactory();
		this.hotspotSizeThreshold = (int)(ont.getLogicalAxiomCount()*0.2); // Default size threshold: 20% |O|
		this.bean = ManagementFactory.getThreadMXBean();
		
		// Initialize data structures
		this.satTimeMap = new HashMap<OWLClass,Double>();
		this.timings = new HashMap<String,Double>();
		this.tempLog = new LinkedHashMap<Approximation,String>();
		this.starModExtractor = new SyntacticLocalityModuleExtractor(man, ont, ModuleType.STAR);
		this.botModExtractor = new SyntacticLocalityModuleExtractor(man, ont, ModuleType.BOT);
	}

	
	/**
	 * 
	 * @param type
	 * @param glassBoxReasoner
	 * @return Set of approximations
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws OWLOntologyCreationException
	 */
	public Set<Approximation> findApproximations(HotspotCandidateType type, boolean glassBoxReasoner) 
			throws IOException, InterruptedException, OWLOntologyCreationException {
		Set<Approximation> approxs = null;
		if(searchStrategy.equals(HotspotSearchStrategy.SEQ)) {
			ArrayList<OWLClass> classes = getIndicators(glassBoxReasoner); // Get SAT-based indicators
			
			if(verbose) System.out.println(" Indicators gathered: " + classes.size());
			approxs = execSequentialHotspotSearch(new HashSet<Hotspot>(), classes, type); // Perform hotspot search based on acquired indicators
		}
		else if(searchStrategy.equals(HotspotSearchStrategy.CON) && indStrategy.equals(HotspotIndicatorStrategy.SAT)) {
			approxs = execConcurrentHotspotSearch(type, glassBoxReasoner);
		}
		else if(searchStrategy.equals(HotspotSearchStrategy.CON) && 
				(indStrategy.equals(HotspotIndicatorStrategy.RANDOM) || indStrategy.equals(HotspotIndicatorStrategy.AD)))
			throw new Error("Feature not implemented");	
		else
			throw new Error("Feature not implemented");
		return approxs;
	}
	
	
	/**
	 * Check whether the given set of approximations is complete w.r.t. atomic subsumptions
	 * @param approxs	Set of approximations
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void verifyHotspotBasedApproximations(Set<Approximation> approxs) throws IOException, InterruptedException {
		if(ontResults == null)
			classifyOriginalOntology();
		
		if(verbose) System.out.println(" -------------------\n Checking completeness of approximations...");
		for(Approximation a : approxs) {
			isComplete(a, ontResults);
		}
	}
	
	
	/**
	 * Classify original ontology
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void classifyOriginalOntology() throws IOException, InterruptedException {
		if(verbose) System.out.println(" -------------------\n Classifying original ontology...");
		ontResults = classify(ontPath, false);
	}
	
	
	/**
	 * Check whether a given approximation is complete w.r.t. atomic subsumptions
	 * @param a	Approximation to be checked
	 * @param r	Classification result of original ontology
	 */
	private boolean isComplete(Approximation a, ClassifierResult r) {
		Set<OWLAxiom> approxEnts = a.getClassifierResults().getEntailments();
		if(a.getHotspot().getClassificationResults() != null)
			approxEnts.addAll(a.getHotspot().getClassificationResults().getEntailments());
		
		Set<OWLAxiom> ontEnts = r.getEntailments();
		Set<OWLAxiom> missingEnts = new HashSet<OWLAxiom>();
		double missing = 0, ontEntNr = ontEnts.size();
		for(OWLAxiom ax : ontEnts) {
			if(!approxEnts.contains(ax)) {
				missingEnts.add(ax);
				missing++;
			}
		}
		double percMissing = (missing/ontEntNr)*100.0;
		if(verbose) System.out.println("    '" + a.getApproximationTypeName() + "' completeness check:\n\tCl(O) = " + ontEnts.size()  + " axioms" +
				"\n\tCl(" + a.getApproximationTypeName() + ") = " + approxEnts.size() + " axioms\n\tMissing " + (int)missing + " entailments (" + (int)percMissing + "%)");
		if(missing == 0) return true;
		else return false;
	}
	
	
	/**
	 * Finds hot spots following a concurrent approach, where, for each 5% of SAT tests done, the searcher fires up as many hot spot
	 * tests as there are available cores. If, at the end, not enough hot spots have been found, a sequential hot spot search will
	 * be triggered, testing all those classes that haven't been checked yet.
	 * @param timeout	
	 * @return Set of hot spots found
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws OWLOntologyCreationException
	 */
	private Set<Approximation> execConcurrentHotspotSearch(HotspotCandidateType type, boolean glassBoxReasoner) 
			throws IOException, InterruptedException, OWLOntologyCreationException {
		if(verbose) System.out.println("\n [Using concurrent hot spot search strategy]");
		Set<Approximation> approxs = new HashSet<Approximation>();
		Set<Hotspot> hotspots = new HashSet<Hotspot>();
		Process p = null;
		
		// Execute SAT tester
		if(!glassBoxReasoner)
			p = execSATTester();
		else
			p = execRegularClassification();
		
		// Read Error output stream first
//		InputStream errorOut = p.getErrorStream();
//		BufferedReader errReader = new BufferedReader(new InputStreamReader(errorOut));
//		String errLine = errReader.readLine();
//		while(errLine != null && !errLine.trim().equals("--EOF--")) {
//			System.out.println(errLine);
//			errLine = errReader.readLine();
//    		if(errLine == null) break;
//		}
		
		InputStream stdout = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

		// Read process output
        String line = reader.readLine();
        double total = 0.0, status = 0.0;
        int nrClasses = ont.getClassesInSignature().size(), cSteps = 1, counter = 0;
        if(glassBoxReasoner) nrClasses *= 2;
        
        // TODO in test
        ArrayList<OWLClass> checked = new ArrayList<OWLClass>();
//        List<OWLClass> checked = Collections.synchronizedList(new ArrayList<OWLClass>());
        
        long start = System.currentTimeMillis();
        
        while(line != null && !line.trim().equals("--EOF--") && hotspots.size() < minHotspots) {
        	if(line.startsWith("<") || line.startsWith("ObjectComplementOf") || line.startsWith("Sat")) {
        		counter++;
        		updateSATMap(line, glassBoxReasoner);
        		double status2 = Math.floor( 100.0 * counter / nrClasses );
        		if(verbose && status < status2) 
        			System.out.println("   " + status2 + "% (" + satTimeMap.size() + "/" + nrClasses + " concepts tested)");
        		
        		// For every 10% of completed SAT tests start hotspot testing
        		if((status2 % 5 == 0.0 && status2 > 0 && status < status2) || satTimeMap.size() % 20 == 0.0) {
        			long end = System.currentTimeMillis();
        			total = (end-start)/1000.0;
        			if(verbose) System.out.println(" Elapsed time: " + total + " seconds. " + counter + "/" + nrClasses + 
        					" classes tested. Concurrent Step: " + cSteps);
        			ArrayList<Hotspot> hotspotList = new ArrayList<Hotspot>();
        			
//        			int toFork = (MAX_PROCESSES/2)-1;
//        			ExecutorService execService = Executors.newFixedThreadPool((MAX_PROCESSES/2)-1);
//        			Set<Future<Hotspot>> futures = new HashSet<Future<Hotspot>>();
//        			
//        			System.out.println("\tForking max of " + toFork + " processes");
//        			
//        			for(int i = 0; i < toFork && i < classes.size()-1; i++) {
//        				OWLClass c = classes.get(i);
//        				if(!checked.contains(c)) {
//        					checked.add(c);
//        					System.out.println("\t Testing hot spot for concept " + ResultsSerializer.getManchesterRendering(c));
//        					ConcurrentHotspotTester tester = new ConcurrentHotspotTester(c, forceHotspotAsModule);
//        					
//        					Future<Hotspot> f = execService.submit(tester);
//        					futures.add(f);
//        				}
//        				else toFork++;
//        			}
//        			
//        			for(Future<Hotspot> f : futures) {
//        				try {
//							hotspotList.add(f.get());
//						} catch (ExecutionException e) {
//							e.printStackTrace();
//						}
//        			}
        			
        			Set<Approximation> apps = execConcurrentStep(hotspotList, sortHashMap(satTimeMap), checked, type);
					if(!apps.isEmpty())
						approxs.addAll(approxs);
					
        			if(!hotspotList.isEmpty()) {
        				hotspots.addAll(hotspotList);
        				for(Hotspot hs : hotspotList) {
        					log += total + "," + hs.getPreparationTime() + "," + hs.getSize() + ",";
        					if(hs.getClassificationTime() != null)
        						log += hs.getClassificationTime() + ",";

        					for(Approximation approx : tempLog.keySet()) {
        						if(approx.getHotspot().equals(hs))
        							log += tempLog.get(approx);
        					}
        					log += "\n,";
        				}
        			}
        			cSteps++;
        			if(verbose) System.out.println("Total nr. of hot spots: " + hotspots.size());
        		}
        		status = status2;
        	}
        	else if(line.startsWith(" SAT"))
        		System.out.println(line);
        	else if(line.startsWith(" Reasoner"))
        		System.out.println(line);
        	
        	line = reader.readLine();
    		if(line == null) break;
        }
        
        p.destroy();
        if(hotspots.size() < minHotspots) {
        	ArrayList<OWLClass> list = sortHashMap(satTimeMap);
        	list.removeAll(checked);
        	execSequentialHotspotSearch(hotspots, list, type);
        }
        
        if(verbose) System.out.println("\n");
        return approxs;
	}
	
	
	/**
	 * 
	 * @param classes
	 * @param checkedClasses
	 * @return Set of verified approximations
	 * @throws OWLOntologyCreationException
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private Set<Approximation> execConcurrentStep(ArrayList<Hotspot> hotspotList, ArrayList<OWLClass> classes, ArrayList<OWLClass> checkedClasses, HotspotCandidateType type) 
			throws OWLOntologyCreationException, IOException, InterruptedException {
		int toFork = MAX_PROCESSES - 1;
		Set<Approximation> goodApproxs = new HashSet<Approximation>();
		for(int i = 0; i < toFork && i < classes.size(); i++) {
			OWLClass c = classes.get(i);
			if(!checkedClasses.contains(c)) {
				checkedClasses.add(c);
				if(verbose) System.out.println("Preparing candidate " + (candidateNr+1) + "\n\tClass: " + ResultsSerializer.getManchesterRendering(c) +
						"\n\tSAT Time: " + satTimeMap.get(c) + " seconds" + "\n\tIndex in sorted list: " + classes.indexOf(c));

				Hotspot candidateHotspot = prepHotspotCandidate(c, type);
				if(candidateHotspot.getAxioms() != null && candidateHotspot.getSize() > 0) {
					Set<Approximation> approxs = null;// = verifyHotspot(candidateHotspot, type); // TODO
					if(!approxs.isEmpty()) {
						goodApproxs.addAll(approxs);
						hotspotList.add(candidateHotspot);
					}
				}
				else if(!classes.isEmpty()) {
					toFork++; continue;
				}
			}
			else if(!classes.isEmpty()) {
				toFork++; continue;
			}
			if(hotspotList.size() >= minHotspots)
				break;
		}
		return goodApproxs;
	}
	
	
	/**
	 * Classify ontology
	 * @return Classification process
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Process execRegularClassification() throws IOException, InterruptedException {
		if(verbose) System.out.println("\n Classifying while listening to SAT times from the reasoner...");
		ArrayList<String> args = new ArrayList<String>();
		args.add("-ont"); 		args.add(ontPath);
		args.add("-reasoner");	args.add(reasonerName);
		args.add("-cl");
		args.add("-b");
		if(verbose) args.add("-v");
		
		return Dispatcher.executeOperation(EntailmentGenerator.class, false, false, args);
	}
	
	
	// TODO
//	class ConcurrentHotspotTester implements Callable<Hotspot> {
//		private OWLClass c;
//		private boolean forceHotspotAsModule;
//
//		/*
//		 * Constructor
//		 */
//		public ConcurrentHotspotTester(OWLClass c, boolean forceHotspotAsModule) {
//			this.c = c;
//			this.forceHotspotAsModule = forceHotspotAsModule;
//		}
//		
//		@Override
//		public Hotspot call() {
//			Hotspot hotspot = null;
//			try {
//				Hotspot candidateHotspot = prepHotspotCandidate(c, forceHotspotAsModule);
//				if(candidateHotspot != null && !candidateHotspot.axioms().isEmpty()) {
//					boolean isHotspot = verifyHotspot(candidateHotspot, true);
//					if(isHotspot)
//						hotspot = candidateHotspot;
//				}
//				else
//					System.out.println("Null Hotspot...");
//			} catch (OWLOntologyCreationException e) {
//				e.printStackTrace();
//			}
//			return hotspot;
//		}
//	}
	
	
	/**
	 * Execute the hot spot searcher in sequential mode
	 * @param hotspots	Set of hotspots
	 * @param classes	Set of classes
	 * @param type	Hotspot candidate type (bottom or star modules)
	 * @param forceHotspotAsModule	Whether the hotspot should necessarily be a (star) module
	 * @return Set of approximations with classification time below the threshold
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws OWLOntologyCreationException
	 */
	private Set<Approximation> execSequentialHotspotSearch(Set<Hotspot> hotspots, ArrayList<OWLClass> classes, HotspotCandidateType type) 
			throws IOException, InterruptedException, OWLOntologyCreationException {
		if(verbose) System.out.println("\n [Using sequential hot spot search strategy]");
		Set<Approximation> goodApproxs = new HashSet<Approximation>();
		for(int i = 0; i < maxTests-1; i++) {
			if(hotspots.size() < minHotspots) {
				if(!classes.isEmpty()) {
					OWLClass c = classes.iterator().next();
					Hotspot candidateHotspot = prepHotspotCandidate(c, type);
					// Hotspot size restriction: 0 stands for restriction-free, otherwise it must be non-empty and smaller than the specified threshold
					if(hotspotSizeThreshold == 0 || (candidateHotspot.getSize() <= hotspotSizeThreshold && candidateHotspot.getSize() > 0) ) {
						// Now get remainder
						ApproximationGenerator gen = new ApproximationGenerator(candidateHotspot, ont);
						boolean testOtherRemainders = false;
						
						Approximation rem = null;
						if(type.equals(HotspotCandidateType.BOTMOD))
							rem = getApproximation(gen, candidateHotspot, ApproximationType.COMPLETE);
						else {
							rem = getApproximation(gen, candidateHotspot, ApproximationType.NAIVE);
							testOtherRemainders = true;
						}
						if(rem != null) { // Remainder is of good size
							if(verbose) System.out.println(" -------------------\n Candidate hotspot " + candidateNr + ". Metrics:\n\tSize: " + candidateHotspot.getSize() + 
									" axioms\n\tPreparation time: " + candidateHotspot.getPreparationTime() + " seconds\n\tSeed concept: " + candidateHotspot.getSeedName());

							boolean isHotspot = testHotspot(candidateHotspot, rem);
							if(isHotspot) {
								gen.setNaiveApproxClassifierResult(rem.getClassifierResults());
								hotspots.add(candidateHotspot);
								goodApproxs.add(rem);
								
								if(verbose) System.out.println("    Classifying hotspot... (timeout: " + classificationThreshold + " milliseconds)");
								candidateHotspot.attachClassifierResults(classify(rem.getHotspotFilePath(), true));
								
								if(testOtherRemainders) {
									Approximation rem1 = getApproximation(gen, candidateHotspot, ApproximationType.COMBINEDCL);
									if(rem1 != null && testHotspot(candidateHotspot, rem1))
										goodApproxs.add(rem1);

									Approximation rem2 = getApproximation(gen, candidateHotspot, ApproximationType.REMCLM);
									if(rem2 != null && testHotspot(candidateHotspot, rem2))
										goodApproxs.add(rem2);

									Approximation rem3 = getApproximation(gen, candidateHotspot, ApproximationType.REMCLMELM);
									if(rem3 != null && testHotspot(candidateHotspot, rem3))
										goodApproxs.add(rem3);
								}
								addTiming("CT(M)", candidateHotspot.getClassificationTime());
								addTiming("CT(Remainder)", rem.getClassifierResults().getReasoningTaskTime());
							}
							candidateNr++;
							if(verbose) System.out.println(" Done");
						}
						else badSizeRems++;
					}
					else badSizeHotspots++;
					classes.remove(c);
				}
				else break;
			}
			else break;
		}
		return goodApproxs;
	}
	
	
	/**
	 * Verify whether the specified concept leads to a hotspot of the given type
	 * @param c	Seed concept
	 * @param type	Hotspot type
	 * @return true if hotspot candidate is a hotspot, false otherwise 
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
//	public boolean verifyHotspot(OWLClass c, HotspotCandidateType type) throws OWLOntologyCreationException, IOException, InterruptedException {
//		Hotspot candidateHotspot = prepHotspotCandidate(c, type);
//		if(sizeThreshold == 0 || candidateHotspot.getSize() <= sizeThreshold) { 
//			Set<Approximation> approx = verifyHotspot(candidateHotspot, type);
//			if(!approx.isEmpty()) return true;
//			else return false;
//		}
//		else {
//			if(verbose) System.out.println("Hotspot too big (" + candidateHotspot.getSize() + " axioms)");
//			return false;
//		}
//	}
	
	
	/**
	 * Prepare candidate hot spot, i.e., extract usage closure for given class (and a module around it) 
	 * @param c	OWLClass
	 * @return Hot spot candidate as an OWLOntology
	 * @throws OWLOntologyCreationException 
	 */
	private Hotspot prepHotspotCandidate(OWLClass c, HotspotCandidateType type) throws OWLOntologyCreationException {
		long start = bean.getCurrentThreadCpuTime();
		
		Set<OWLAxiom> hotspotCandidate = null;
		if(type.equals(HotspotCandidateType.BOTMOD))
			hotspotCandidate = getBottomModHotspotCandidate(c);
		else if(type.equals(HotspotCandidateType.STARMOD))
			hotspotCandidate = getStarModHotspotCandidate(c);
		else if(type.equals(HotspotCandidateType.USAGE))
			hotspotCandidate = getUsageHotspotCandidate(c);
		
		long end = bean.getCurrentThreadCpuTime();
		double total = (end-start)/1000000000.0;
		
		return new Hotspot(man, hotspotCandidate, total, c, candidateNr);
	}
	
	
	/**
	 * Get Star-Module hotspot candidate based on given seed concept
	 * @param c	Seed concept
	 * @return Hotspot candidate
	 */
	private Set<OWLAxiom> getStarModHotspotCandidate(OWLClass c) {
		Set<OWLEntity> sig = new HashSet<OWLEntity>();
		Set<OWLAxiom> usageClosure = getUsageHotspotCandidate(c);
		for(OWLAxiom a : usageClosure)
			sig.addAll(a.getSignature());
		return starModExtractor.extract(sig);
	}
	
	
	/**
	 * Get Bottom-Module hotspot candidate based on given seed concept
	 * @param c	Seed concept
	 * @return Hotspot candidate
	 */
	private Set<OWLAxiom> getBottomModHotspotCandidate(OWLClass c) {
		Set<OWLEntity> sig = new HashSet<OWLEntity>();
		sig.add(c);
		return botModExtractor.extract(sig);
	}
	
	
	/**
	 * Get usage closure hotspot candidate based on given concept
	 * @param c	Seed concept
	 * @return Hotspot candidate
	 */
	private Set<OWLAxiom> getUsageHotspotCandidate(OWLClass c) {
		return ont.getReferencingAxioms(c);
	}
	
	
	/**
	 * Test whether a given candidate hot spot is indeed a hot spot according
	 * to the classificationThreshold parameter
	 * @param candidateHotspot	Hot spot candidate
	 * @param index	Candidate number
	 * @return true if the given candidate is indeed a hot spot
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private boolean testHotspot(Hotspot candidateHotspot, Approximation rem) {
		if(verbose) System.out.println("  --- \n  Testing '" + rem.getApproximationTypeName() + "' approximation. Metrics:");
		if(verbose) System.out.println("\tSize: " + rem.getApproximationSize() + " axioms\n\tPreparation time: " + rem.getPreparationTime() + " seconds");
		
		String remainderPath = rem.getApproximationFilePath();
		String hotspotPath = rem.getHotspotFilePath();
		boolean isHotspot;

		if(verbose) System.out.println("    Classifying remainder... (timeout: " + classificationThreshold + " milliseconds)");

		ClassifierResult result = null;
		try {
			result = classify(remainderPath, true);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		rem.attachClassifierResult(result);
		
		if(result.timedOut()) isHotspot = false;
		else isHotspot = true;

		if(!isHotspot) {
			// Delete serialized files
			new File(remainderPath).delete();
			new File(hotspotPath).delete();
			
			System.out.println("\tRemainder is too slow... ");
			
			Double d = classificationThreshold/1000.0;
			addTiming("Remainder Tests", d);
		}
		else
			tempLog.put(rem, rem.getPreparationTime() + "," + rem.getApproximationSize() + "," + result.getReasoningTaskTime() + "," + result.getNumberOfEntailments() + ",");
		return isHotspot;
	}
	
	
	/**
	 * Classify ontology located in the specified path
	 * @param ontologyPath	File path to ontology
	 * @return Classification results
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public ClassifierResult classify(String ontologyPath, boolean useTimeout) throws IOException, InterruptedException {
		ArrayList<String> args = new ArrayList<String>();
		args.add("-ont"); 		args.add(ontologyPath);
		args.add("-reasoner");	args.add(reasonerName);
		args.add("-o");			args.add(outputDir);
		args.add("-cl");
		args.add("-b");
		args.add("-v");
		if(useTimeout) {args.add("-t"); args.add(classificationThreshold + "");}

		Process p = Dispatcher.executeOperation(EntailmentGenerator.class, false, false, args);
		InputStream stdout = p.getInputStream();
		
		// Desired output 
		double clTime = 0; 
		int nrEnts = 0, nrUnsat = 0;
		boolean isConsistent = true, timedOut = false;
		String infSubsFile = "";
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
		String line = reader.readLine();
		while (line != null && !line.trim().equals("--EOF--")) {
			if(line.contains("Aborted") || line.contains("Exception") || line.contains("Error")) {
				System.out.println(line);
				timedOut = true;
				break;
			}
			if(line.contains("Classification time")) {
				clTime = Double.parseDouble(line.substring(line.indexOf(":")+2, line.indexOf(" seconds")));
				if(verbose) System.out.println("\tClassification time: " + clTime + " seconds");
			}
			if(line.contains("Nr. Entailments")) {
				nrEnts = Integer.parseInt(line.substring(line.indexOf(":")+2, line.length()));
				if(verbose) System.out.println("\tNr. Entailments: " + nrEnts);
			}
			if(line.contains("Nr. Unsatisfiable Classes")) {
				nrUnsat = Integer.parseInt(line.substring(line.indexOf(":")+2, line.length()));
				if(verbose) System.out.println("\tNr. Unsatisfiable classes: " + nrUnsat);
			}
			if(line.equals("Ontology is inconsistent")) {
				isConsistent = false;
				if(verbose) System.out.println("\tOntology is inconsistent");
			}
			if(line.contains("Classification results saved to")) {
				infSubsFile = line.substring(line.indexOf(":")+2, line.length());
			}
			line = reader.readLine();
			if(line == null)
				break;
		}
		
		Set<OWLAxiom> entailments = new HashSet<OWLAxiom>();
		if(!timedOut && !infSubsFile.equals("")) {
			OWLOntology infSubs = null;
			try {
				infSubs = new OntologyLoader(new File(infSubsFile), false).loadOntology();
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			}
			if(infSubs != null) entailments = infSubs.getAxioms();
		}
		return new ClassifierResult(entailments, clTime, nrUnsat, "", isConsistent, timedOut);
	}
	
	
	/**
	 * Given a candidate hot spot and its number, generate an approximation, serialize the hotspot and 
	 * corresponding approximation, and return the absolute path where the approximation is serialized to
	 * @param candidateHotspot	Hot spot candidate
	 * @param index	Candidate number
	 * @param type	Approximation type
	 * @return	Absolute path where candidate approximation is serialized
	 */
	private Approximation getApproximation(ApproximationGenerator gen, Hotspot candidateHotspot, ApproximationType type) {
		long start = bean.getCurrentThreadCpuTime();
		Set<OWLAxiom> remainderAxioms = gen.getApproximationAxioms(type, reasonerName);
		long end = bean.getCurrentThreadCpuTime();
		double total = (end-start)/1000000000.0;
		
		// Remainder size restriction
		if(remainderAxioms.size() < ont.getLogicalAxiomCount()) {
			String candidatePath = outputDir + "Hotspots_" + reasonerName + File.separator;
			if(candidatePath.contains(".owl"))
				candidatePath = candidatePath.replaceAll(".owl", "");

			String approxFile = candidatePath + "remainder" + candidateNr + "_" + type.toString() + ".owl";
			String hsFile = candidatePath + "hotspot" + candidateNr + ".owl";
			candidateHotspot.setPath(hsFile);
			try {
				man.saveOntology(man.createOntology(remainderAxioms), new OWLXMLOntologyFormat(), IRI.create("file:" + approxFile));
				man.saveOntology(candidateHotspot.getHotspotAsOntology(), new OWLXMLOntologyFormat(), IRI.create("file:" + hsFile));
			} catch (OWLOntologyStorageException | OWLOntologyCreationException e) {
				e.printStackTrace();
			}
			return new Approximation(remainderAxioms, approxFile, candidateHotspot, hsFile, total, remainderAxioms.size(), type);
		}
		else {
			System.out.println("\tApproximation " + type.name() + " too big...");
			return null;
		}
	}
		
	
	/**
	 * Get a list of indicator classes (hot spot seeds) depending on the search strategy
	 * @return List of indicator classes
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private ArrayList<OWLClass> getIndicators(boolean glassBoxReasoner) throws IOException, InterruptedException {
		ArrayList<OWLClass> classes = null;
		if(indStrategy.equals(HotspotIndicatorStrategy.SAT))
			classes = performSATTest(glassBoxReasoner);
		else if(indStrategy.equals(HotspotIndicatorStrategy.RANDOM)) {
			classes = new ArrayList<OWLClass>(ont.getClassesInSignature());
			Collections.shuffle(classes);
		}
		else if(indStrategy.equals(HotspotIndicatorStrategy.AD)) {
			throw new Error("Feature not implemented");
		}
		return classes;
	}
	
	
	/**
	 * Performs SAT tests for all classes in the ontology signature
	 * @param timeout	Timeout for entire SAT checking operation
	 * @return List of classes in descending order of SAT checking time
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private ArrayList<OWLClass> performSATTest(boolean glassBoxReasoner) throws IOException, InterruptedException {
		Process p = null;
		if(glassBoxReasoner) p = execRegularClassification();
		else p = execSATTester();
		
		InputStream stdout = p.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

		Set<OWLClass> classes = ont.getClassesInSignature();
		classes.remove(df.getOWLThing());
		classes.remove(df.getOWLNothing());
		
        String line = reader.readLine();
        double status = 0;
        double counter = 0, nrClasses = classes.size();
        if(glassBoxReasoner) nrClasses *= 2;

        while(line != null && !line.trim().equals("--EOF--")) {
//        	System.out.println("[debug] " + line);
        	if(line.startsWith("<") || line.startsWith("ObjectComplementOf")) {
        		counter++;
        		updateSATMap(line, glassBoxReasoner);
        		int status2 = (int)(100*(counter/nrClasses));
        		if(status < status2)
        			System.out.println("   " + status2 + "% (" + (int)counter + "/" + (int)nrClasses + " tests done)");
        		status = status2;
        	}
        	else if(line.startsWith("SAT")) {
        		System.out.println(" Done. " + line);
        		Double time = Double.valueOf(line.substring(line.indexOf(":")+2, line.lastIndexOf(" s")));
        		addTiming("SAT Test", time);
        	}
        	
        	line = reader.readLine();
    		if (line == null)
    			break;
        }
		return sortHashMap(satTimeMap);
	}
	
	
	/**
	 * Update the SAT tests time map
	 * @param line	Line of input stream reader
	 */
	private void updateSATMap(String line, boolean glassBoxReasoner) {
		String[] results = line.split(" ");		
		OWLClass c = null;
		String ce = results[0];
		if(ce.contains("ObjectComplementOf") || ce.contains("SatN") || ce.contains("SatP"))
			ce = ce.substring(ce.indexOf("(")+1, ce.indexOf(")"));
		
		ce = ce.replace(">", "");
		ce = ce.replace("<", "");
		
		c = df.getOWLClass(IRI.create(ce));
		double time;
		if(results[1].contains("timedout")) time = csatTimeout;
		else if(results[1].contains("error")) time = 0;
		else time = Double.parseDouble(results[1]);
		satTimeMap.put(c, time);
	}
	
	
	/**
	 * Execute SATTester class
	 * @return SATTester process
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Process execSATTester() throws IOException, InterruptedException {
		System.out.println(" -------------------\n Performing SAT tests...");
		ArrayList<String> args = new ArrayList<String>();
		args.add("-ont");			args.add(ontPath);
		args.add("-reasoner");		args.add(reasonerName);
		args.add("-o");				args.add(outputDir);
		args.add("-t");				args.add(classificationThreshold + ""); // SAT timeout would be original classification time (or timeout)
		args.add("-b");
		args.add("-v");
		if(csatTimeout != 0) { args.add("-i"); args.add(csatTimeout + "");}
		
		Process p = Dispatcher.executeOperation(SATOntologyTester.class, false, false, args);
        return p; 
	}
	
	
	/**
	 * Sort hashmap in descending order of values (sat time)
	 */
	private ArrayList<OWLClass> sortHashMap(HashMap<OWLClass, Double> times) {
		ArrayList<OWLClass> list = new ArrayList<OWLClass>();

		List<OWLClass> keylist = new ArrayList<OWLClass>(times.keySet());
		List<Double> valuelist = new ArrayList<Double>(times.values());
		
		ArrayList<Double> sortedValueList = new ArrayList<Double>(times.values());
		Collections.sort(sortedValueList);
		Collections.reverse(sortedValueList);
		
		for(int i = 0; i < sortedValueList.size(); i++) {
			OWLClass c = (OWLClass) keylist.get(valuelist.indexOf(sortedValueList.get(i)));
			list.add(c);
		}
	
		return list;
	}
	
	
	/**
	 * Hotspot indicator strategies
	 */
	public enum HotspotIndicatorStrategy {
		SAT ("SAT-Based Search"),
		RANDOM ("Random Concept Picking Search"),
		AD ("Atomic Decomposition Based Search");
		
		String name;
		HotspotIndicatorStrategy(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
	
	
	/**
	 * Hotspot search strategies
	 */
	public enum HotspotSearchStrategy {
		SEQ ("Sequential search strategy"),
		CON ("Concurrent search strategy");
		
		String name;
		HotspotSearchStrategy(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
	
	
	/**
	 * Hotspot candidate type
	 */
	public enum HotspotCandidateType {
		BOTMOD ("Bottom locality-based module"),
		STARMOD ("Top-Bottom-Star locality-based module"),
		USAGE ("Usage closure of a given concept");
		
		String name;
		HotspotCandidateType(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
	
	
	/**
	 * Serialize approximations' classification results
	 * @param approxs	Set of approximations
	 */
	private void serializeApproximationClassHierarchy(Set<Approximation> approxs) {
		int aCounter = 1;
		for(Approximation a : approxs) {
			Set<OWLAxiom> classHierarchy = new HashSet<OWLAxiom>();
			
			if(a.getApproximationType().equals(ApproximationType.COMPLETE))
				classHierarchy.addAll(a.getRemainderAndHotspotEntailments());
			else
				classHierarchy.addAll(a.getClassifierResults().getEntailments());
			
			String filename = outputDir + "HSF_ClassHierarchy_" + aCounter + ".owl";
			if(verbose) System.out.println(" Serialized class hierarchy (" + classHierarchy.size() + " entailments) as: " + filename);
			try {
				man.saveOntology(man.createOntology(classHierarchy), new OWLXMLOntologyFormat(), IRI.create("file:" + filename));
			} catch (OWLOntologyStorageException | OWLOntologyCreationException e) {
				e.printStackTrace();
			}
			aCounter++;
		}
	}
	
	
	/**
	 * Get log file
	 * @return Log file
	 */
	public String getLog() {
		return log;
	}
	
	
	/**
	 * Produce a log of hotspot finding timings
	 */
	public void produceLog(Set<Approximation> approxs) {
		String header = "\nOntology,Reasoner,SAT Time,Hotspot Finding Time,Slow Remainders Testing Time,Bad Size Hotspots,Bad Size Remainders,";
		
		Double sat = timings.get("SAT Test");
		Double hsf = timings.get("Hotspot Search");
		Double remTests = timings.get("Remainder Tests");
		if(remTests == null) remTests = 0.0;
		
		String row = ontPath.substring(ontPath.lastIndexOf("/")+1, ontPath.length()) + "," + reasonerName + "," + sat + "," + hsf + "," + remTests 
				+ "," + badSizeHotspots + "," + badSizeRems + ",";
		
		for(Approximation a : approxs) {
			Hotspot h = a.getHotspot();
			header += "Hotspot Candidate Nr.,Hotspot Seed Concept,";
			row += h.getCandidateNr() + "," + h.getSeedName() + ",";
			
			ClassifierResult r = h.getClassificationResults();
			if(r != null) {
				header += "CT(M),AtSubs(M),";
				row += h.getClassificationTime() + "," + h.getClassificationResults().getNumberOfEntailments() + ",";
			}
			
			ClassifierResult ar = a.getClassifierResults();
			header += "CT(Remainder),AtSubs(Remainder),";
			row += ar.getReasoningTaskTime() + "," + ar.getNumberOfEntailments() + ",";
			
			header += "CT(Compilation),AtSubs(Compilation),";
			if(a.getApproximationType().equals(ApproximationType.COMPLETE))
				row += (ar.getReasoningTaskTime()+r.getReasoningTaskTime()) + "," + a.getRemainderAndHotspotEntailments().size() + ",";
			else
				row += ar.getReasoningTaskTime() + "," + ar.getNumberOfEntailments() + ",";
			
			// Signature overlap between hotspot and remainder
			int partsSigOverlap = 0;
			Set<OWLEntity> hotspotSig = h.getSignature();
			Set<OWLEntity> remainderSig = a.getSignature();
			Set<OWLEntity> ontSig = ont.getSignature();
			for(OWLEntity e : hotspotSig) {
				if(remainderSig.contains(e))
					partsSigOverlap++;
			}
			
			// Signature overlap between ontology and remainder
			int ontRemainderSigOverlap = 0;
			for(OWLEntity e : remainderSig) {
				if(ontSig.contains(e))
					ontRemainderSigOverlap++;
			}
			
			// Axiom overlap between hotspot and remainder
			int partsAxiomOverlap = 0;
			for(OWLAxiom ax : h.getAxioms()) {
				if(a.getApproximation().contains(ax))
					partsAxiomOverlap++;
			}
			
			// Entailment overlap between hotspot and remainder 
			int partsEntailmentOverlap = 0;
			for(OWLAxiom ax : r.getEntailments()) {
				if(ar.getEntailments().contains(ax))
					partsEntailmentOverlap++;
			}
			header += "Nr. Concepts in sig(M) and sig(Remainder),Nr. Concepts in sig(Remainder) and sig(O)," +
					"Nr. Axioms in Remainder and Hotspot,Nr. Entailments from Remainder and Hotspot,";
			row += partsSigOverlap + "," + ontRemainderSigOverlap + "," + partsAxiomOverlap + "," + partsEntailmentOverlap + ",";
		}

		if(verbose) {
			System.out.println(" -------------------\n Hotspot Finder Metrics:");
			System.out.println("    Bad size hotspots: " + badSizeHotspots);
			System.out.println("    Bad size remainders: " + badSizeRems);
		}
		
		Double total = 0.0;
		for(String s : timings.keySet()) {
			Double d = timings.get(s);
			if(verbose) System.out.println("    " + s + ": " + d + " seconds");
			total += d;
		}
		header += "Total Time";
		row += total;
		log = header + "\n" + row;
		
		if(verbose) System.out.println(" -------------------\n Total: " + total + " seconds");
	}
	
	
	/**
	 * Add timing to timings map
	 * @param s	Description
	 * @param d	Time (in seconds)
	 */
	public void addTiming(String s, Double d) {
		if(timings.keySet().contains(s)) {
			Double d0 = timings.get(s);
			d0 += d;
			timings.put(s, d0);
		}
		else
			timings.put(s, d);
	}
	
	
	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, IOException, InterruptedException {
		System.out.println("Executing Hot Spot Finder...");
		String ontFile = null, reasonerName = null, outputDir = null, indStrat = "SAT", searchStrat = "SEQ", hsType = "STAR";
		boolean verbose = false, ignoreAbox = false;
		int minHotspots = 0, maxTests = 0, sizeThreshold = 0;
		long timeout = 0, indTimeout = 0;
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-ont"))		ontFile = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			if(arg.equalsIgnoreCase("-t"))			timeout = Long.parseLong(args[++i].trim());
			if(arg.equalsIgnoreCase("-i"))			indTimeout = Long.parseLong(args[++i].trim());
			if(arg.equalsIgnoreCase("-n"))			minHotspots = Integer.parseInt(args[++i].trim());
			if(arg.equalsIgnoreCase("-m"))			maxTests = Integer.parseInt(args[++i].trim());
			if(arg.equalsIgnoreCase("-s"))			indStrat = args[++i].trim();
			if(arg.equalsIgnoreCase("-p"))			searchStrat = args[++i].trim();
			if(arg.equalsIgnoreCase("-a"))			hsType = args[++i].trim();
			if(arg.equalsIgnoreCase("-o"))			outputDir = args[++i].trim();
			if(arg.equalsIgnoreCase("-b"))			ignoreAbox = true;
			if(arg.equalsIgnoreCase("-v"))			verbose = true;
		}
		
		HotspotIndicatorStrategy indStrategy = null;
		if(indStrat.equalsIgnoreCase("sat"))			indStrategy = HotspotIndicatorStrategy.SAT;
		else if(indStrat.equalsIgnoreCase("ad"))		indStrategy = HotspotIndicatorStrategy.AD;
		else if(indStrat.equalsIgnoreCase("random"))	indStrategy = HotspotIndicatorStrategy.RANDOM;
		else throw new Error("Unrecognized hot spot indicator strategy: " + indStrat + 
					". Accepted values are SAT, AD, or Random");
		
		HotspotSearchStrategy searchStrategy = null;
		if(searchStrat.equalsIgnoreCase("seq"))			searchStrategy = HotspotSearchStrategy.SEQ;
		else if(searchStrat.equalsIgnoreCase("con"))	searchStrategy = HotspotSearchStrategy.CON;
		else throw new Error("Unrecognized hot spot search strategy: " + indStrat + 
					". Accepted values are SEQ or CON");
		
		HotspotCandidateType hotspotType = null;
		if(hsType.equalsIgnoreCase("bot"))				hotspotType = HotspotCandidateType.BOTMOD;
		else if(hsType.equalsIgnoreCase("star"))		hotspotType = HotspotCandidateType.STARMOD;
		else if(hsType.equalsIgnoreCase("usg"))			hotspotType = HotspotCandidateType.USAGE;
		else throw new Error("Unrecognized hotspot candidate type: " + hsType + 
				". Accepted values are BOT, STAR or USG");
		
		if(ontFile != null && reasonerName != null && indStrategy != null) {
			File f = new File(ontFile);
			OWLOntology ont = new OntologyLoader(f, verbose).loadOntology(ignoreAbox);
		
			HotspotFinder finder = new HotspotFinder(ont, reasonerName, f.getAbsolutePath(), outputDir, timeout, indStrategy, searchStrategy, verbose);
			
			if(sizeThreshold != 0) finder.hotspotSizeThreshold = sizeThreshold;
			if(indTimeout != 0) finder.csatTimeout = indTimeout;
			if(minHotspots != 0) finder.minHotspots = minHotspots;
			if(maxTests != 0) finder.maxTests = maxTests;
			
			finder.hotspotSizeThreshold = 0;
			long start = finder.bean.getCurrentThreadCpuTime();
			
			Set<Approximation> approxs = finder.findApproximations(hotspotType, false);
			
			long end = finder.bean.getCurrentThreadCpuTime();
			double total = (end-start)/1000000000.0;
			
			finder.addTiming("Hotspot Search", total);
			finder.produceLog(approxs);
			
			if(!approxs.isEmpty()) {
				finder.serializeApproximationClassHierarchy(approxs);
//				finder.verifyHotspotBasedApproximations(approxs);
			}
			
			FileWriter writer = new FileWriter(new File(outputDir + "HSF_Log.csv"), true);
			writer.append(finder.getLog());
			writer.close();
		}
		else throw new RuntimeException("Error: Minimum parameters are: -ont OntologyFilePath -reasoner ReasonerName.\n" +
					"\tPlease review the usage information via the -h flag.");
	}
}