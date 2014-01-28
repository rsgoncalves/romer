package uk.ac.manchester.cs.romer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import javax.management.RuntimeErrorException;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import eu.trowl.owlapi3.rel.reasoner.dl.RELReasonerFactory;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ReasonerLoader {
	private ThreadMXBean bean;
	private String reasonerName, reasonerVersion;
	private OWLOntology ont;
	private boolean verbose;
	private Long timeout;
	private double creationTime;
	

	/**
	 * ReasonerLoader constructor
	 * @param reasonerName	Reasoner name
	 * @param ont	OWL ontology
	 * @param verbose	Verbose mode
	 */
	public ReasonerLoader(String reasonerName, OWLOntology ont, boolean verbose) {
		bean = ManagementFactory.getThreadMXBean();
		this.reasonerName = reasonerName;
		this.ont = ont;
		this.verbose = verbose;
	}
	
	
	/**
	 * ReasonerLoader constructor with timeout
	 * @param reasonerName	Reasoner name
	 * @param ont	OWL ontology
	 * @param timeout	Timeout for basic reasoner tasks (in milliseconds)
	 * @param verbose	Verbose mode
	 */
	public ReasonerLoader(String reasonerName, OWLOntology ont, long timeout, boolean verbose) {
		bean = ManagementFactory.getThreadMXBean();
		this.reasonerName = reasonerName;
		this.ont = ont;
		this.timeout = timeout;
		this.verbose = verbose;
	}
	
	
	/**
	 * Get reasoner creation time (in seconds)
	 * @return Reasoner creation time (in seconds)
	 */
	public double getReasonerCreationTime() {
		return creationTime;
	}
	
	
	/**
	 * Get reasoner version
	 * @return Reasoner version, where reported
	 */
	public String getReasonerVersion() {
		return reasonerVersion;
	}

	
	public OWLReasoner getReasoner() {
		OWLReasonerConfiguration config = null;
		if(timeout != null)  config = new SimpleConfiguration(timeout);
		else config = new SimpleConfiguration();
		OWLReasoner reasoner = getReasoner(config);
		if(verbose)
			System.out.println(" Reasoner: " + reasonerName + " v" + reasonerVersion + ". Creation time: " + creationTime + " seconds");
		return reasoner;
	}
	
	
	/**
	 * Create an OWL reasoner
	 * @return OWL Reasoner
	 */
	public OWLReasoner getReasoner(OWLReasonerConfiguration config) {
//		OWL2ProfileChecker checker = new OWL2ProfileChecker(ont);
		OWLReasonerFactory reasonerFactory = null;
		OWLReasoner reasoner = null;
		
		if(reasonerName.equalsIgnoreCase("hermit"))
			reasonerFactory = new Reasoner.ReasonerFactory();
		else if(reasonerName.equalsIgnoreCase("fact"))
			reasonerFactory = new FaCTPlusPlusReasonerFactory();			
		else if(reasonerName.equalsIgnoreCase("pellet"))
			reasonerFactory = new PelletReasonerFactory(); 
		else if(reasonerName.equalsIgnoreCase("jfact"))
			reasonerFactory = new JFactFactory();
//		else if(reasonerName.equalsIgnoreCase("elk")) {
//			if(checker.isOWL2EL(ont))
//				reasonerFactory = new ElkReasonerFactory();
//			else
//				throw new RuntimeException("Cannot initialize reasoner: Input ontology is not in the OWL 2 EL profile." +
//						" Please choose an appropriate reasoner.");
//		}
//		else if(reasonerName.equalsIgnoreCase("snorocket")) {
//			if(checker.isOWL2EL(ont))
//				reasonerFactory = new SnorocketReasonerFactory();
//			else
//				throw new RuntimeException("Cannot initialize reasoner: Input ontology is not in the OWL 2 EL profile." +
//						" Please choose an appropriate reasoner.");
//		}
//		else if(reasonerName.equalsIgnoreCase("jcel")) {
//			if(checker.isOWL2EL(ont))
//				reasonerFactory = new JcelReasonerFactory();
//			else
//				throw new RuntimeException("Cannot initialize reasoner: Input ontology is not in the OWL 2 EL profile." +
//						" Please choose an appropriate reasoner.");
//		}
		else if(reasonerName.equalsIgnoreCase("trowl"))
			reasonerFactory = new RELReasonerFactory();
		else {
			throw new RuntimeErrorException(new Error("Unknown reasoner: " + reasonerName + ". " +
					"Valid reasoners: Hermit | Fact | Pellet | JFact | ELK | jcel | SnoRocket ")); 
		}

		if(reasonerFactory != null) {
			long start = bean.getCurrentThreadCpuTime();

			reasoner = reasonerFactory.createReasoner(ont, config);

			long end = bean.getCurrentThreadCpuTime();
			creationTime = (end-start)/1000000000.0;
			
			if(reasoner != null) {
				if(!reasonerName.equalsIgnoreCase("snorocket") && !reasonerName.equalsIgnoreCase("elk") &&
						!reasonerName.equalsIgnoreCase("jfact"))
					reasonerVersion = reasoner.getReasonerVersion().getMajor() + "." + reasoner.getReasonerVersion().getMinor() 
						+ "." + reasoner.getReasonerVersion().getPatch();
				else
					reasonerVersion = "[unreported]";
			}
		}
		return reasoner; 
	}
}
