package uk.ac.manchester.cs.romer.utils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import uk.ac.manchester.cs.romer.OntologyLoader;
import uk.ac.manchester.cs.romer.reasonertasks.SATOntologyTester;
import uk.ac.manchester.cs.romer.reasonertasks.SATResult;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class SATBasedCandidateExtractor {
	private OWLOntology ont;
	private SyntacticLocalityModuleExtractor me;
	
	public SATBasedCandidateExtractor(OWLOntology ont) {
		this.ont = ont;
		me = new SyntacticLocalityModuleExtractor(ont.getOWLOntologyManager(), ont, ModuleType.BOT);
	}
	
	
	/**
	 * Sort hashmap in descending order of values (sat time)
	 */
	public static ArrayList<OWLClass> sortHashMap(HashMap<OWLClass, Double> times) {
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
	

	public static <K, V extends Comparable<? super V>> Map<K,V> sortByValue(Map<K,V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>( map.entrySet() );
		Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
			public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ) {
				return (o1.getValue()).compareTo( o2.getValue() );
			}
		} );
		Collections.reverse(list);
		
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put( entry.getKey(), entry.getValue() );
		}
		
		return result;
	}
	
	
	public OWLOntology extractModule(OWLEntity c) throws OWLOntologyCreationException {
		return me.extractAsOntology(Collections.singleton(c), IRI.create(UUID.randomUUID().toString()));
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public OWLOntology extractModule(Set c) throws OWLOntologyCreationException {
		return me.extractAsOntology(c, IRI.create(UUID.randomUUID().toString()));
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		String ontPath = null, reasonerName = null, outputDir = null;
		long opTimeout = 0, cSatTimeout = 0;
		boolean verbose = false, fork = true, ignoreAbox = false;

		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-ont"))		ontPath = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			if(arg.equalsIgnoreCase("-i"))			cSatTimeout = new Double(args[++i].trim()).longValue();
			if(arg.equalsIgnoreCase("-o"))			outputDir = args[++i].trim();
			if(arg.equalsIgnoreCase("-b"))			ignoreAbox = true;
			if(arg.equalsIgnoreCase("-v"))			verbose = true;
		}

		if(ontPath != null && reasonerName != null) {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			File ontFile = new File(ontPath);
			OWLOntology ont = new OntologyLoader(ontFile, verbose).loadOntology(ignoreAbox);
			SATOntologyTester satTester = new SATOntologyTester(ont, reasonerName, false, outputDir, verbose);
			
			long start = bean.getCurrentThreadCpuTime();
			
			Set<SATResult> results = satTester.testCSATinIsolation(reasonerName, cSatTimeout);
			
			long end = bean.getCurrentThreadCpuTime();
			double total = (end-start)/1000000000.0;
			if(verbose) System.out.println("SAT testing time: " + total + " seconds");
			
			String outPath = ontFile.getName();
			outPath = outPath.replace(".owl", "");
			outPath = outPath.replace(".xml", "");
			outPath = "/Volumes/Data/KCompil/BioPortal_Nov12/" + outPath ;
			
			satTester.serializeResults(results, outPath, "SAT_" + reasonerName + ".csv");
			
			SATBasedCandidateExtractor ce = new SATBasedCandidateExtractor(ont); 
			
			HashMap<OWLClass,Double> map = new HashMap<OWLClass,Double>();
			for(SATResult r : results) {
				map.put((OWLClass) r.getConcept(), r.getSatTestTime());
			}
			Map<OWLClass,Double> sorted = sortByValue(map);
//			List<OWLClass> l = sortHashMap(map);
			int counter = 1;
			for(OWLClass c : sorted.keySet()) {
				if(counter <= 5) {
					System.out.println("Concept: " + c);
					OWLOntology hs_cand = ce.extractModule(c);
					System.out.println("\tHostpot candidate has " + hs_cand.getLogicalAxiomCount() + " axioms");
					hs_cand.getOWLOntologyManager().saveOntology(hs_cand, IRI.create("file:" + outPath + "/Candidate_" + counter + "/Hotspot_" + reasonerName + ".owl"));

					Set<OWLClass> ontSig = ont.getClassesInSignature();
					ontSig.removeAll(hs_cand.getClassesInSignature());

					OWLOntology rem_cand = ce.extractModule(ontSig);
					System.out.println("\tRemainder candidate has " + rem_cand.getLogicalAxiomCount() + " axioms");
					rem_cand.getOWLOntologyManager().saveOntology(rem_cand, IRI.create("file:" + outPath + "/Candidate_" + counter + "/Remainder_" + reasonerName + ".owl"));
					counter++;
				}
			}
		}
		else
			throw new RuntimeException("Error: Minimum parameters are: -ont OntologyFilePath -reasoner ReasonerName.\n" +
					"\tPlease review the usage information via the -h flag.");
	}
}
