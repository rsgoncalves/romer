/*******************************************************************************
 * This file is part of romer.
 * 
 * romer is distributed under the terms of the GNU Lesser General Public License (LGPL), Version 3.0.
 *  
 * Copyright 2011-2014, The University of Manchester
 *  
 * romer is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *  
 * romer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even 
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
 * General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License along with romer.
 * If not, see http://www.gnu.org/licenses/.
 ******************************************************************************/
package uk.ac.manchester.cs.romer.reasonertasks;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import uk.ac.manchester.cs.romer.OntologyLoader;
import uk.ac.manchester.cs.romer.ReasonerLoader;
import uk.ac.manchester.cs.romer.utils.ResultsSerializer;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class SATOntologyTester {
	private OWLOntology ont;
	private ThreadMXBean bean;
	private boolean doNegativeTests, verbose;
	private String reasonerName, outputDir;
	
	/**
	 * Constructor
	 * @param ont	OWLOntology
	 * @param doNegativeTests	Perform SAT tests of negated concepts
	 * @param verbose	Verbose mode
	 */
	public SATOntologyTester(OWLOntology ont, String reasonerName, boolean doNegativeTests, String outputDir, boolean verbose) {
		this.ont = ont;
		this.reasonerName = reasonerName;
		this.doNegativeTests = doNegativeTests;
		this.verbose = verbose;
		this.outputDir = outputDir;
		this.bean = ManagementFactory.getThreadMXBean();
	}
	
	
	/**
	 * Define the list of concepts to perform SAT tests
	 * @param ont	OWLOntology
	 * @return Set of concepts to test satisfiability
	 */
	private Set<OWLClassExpression> defineConceptList(OWLOntology ont) {
		OWLDataFactory df = OWLManager.getOWLDataFactory();
		Set<OWLClassExpression> concepts = new HashSet<OWLClassExpression>();
		for(OWLClass c : ont.getClassesInSignature()) {
			concepts.add(c);
			if(doNegativeTests) concepts.add(getNegatedConcept(c, df));
		}
		return concepts;
	}
	
	
	/**
	 * Get the negation of a given concept
	 * @param c	Concept 
	 * @param df	OWLDataFactory
	 * @return Set of concepts to test satisfiability 
	 */
	private OWLClassExpression getNegatedConcept(OWLClassExpression c, OWLDataFactory df) {
		return df.getOWLObjectComplementOf(c);
	}
	
	
	/**
	 * Test satisfiability of all atomic concepts with per-SAT-test timeout
	 * @param reasonerName	Name of reasoner to be used
	 * @param cSatTimeout 	Timeout in milliseconds
	 * @return Set of SAT results 
	 */
	public Set<SATResult> testCSATwithIndividualTimeout(String reasonerName, long cSatTimeout) {
		if(verbose) System.out.println(" Individual tests timeout: " + cSatTimeout + " milliseconds");
		this.reasonerName = reasonerName;
		OWLReasoner reasoner = new ReasonerLoader(reasonerName, ont, cSatTimeout, verbose).getReasoner();
		return testCSAT(reasoner);
	}
	
	
	/**
	 * Test satisfiability of all atomic concepts with general timeout
	 * @param reasonerName	Reasoner name
	 * @param opTimeout	Timeout in milliseconds
	 * @param cSatTimeout	Individual timeout for SAT checks
	 * @return Set of SAT results 
	 */
	public Set<SATResult> testCSATwithOverallTimeout(String reasonerName, long opTimeout, long cSatTimeout) {
		if(verbose) System.out.println(" Overall timeout: " + opTimeout + " milliseconds");
		this.reasonerName = reasonerName;
		OWLReasoner reasoner = null;
		if(cSatTimeout != 0) {
			if(verbose) System.out.println(" Individual tests timeout: " + cSatTimeout + " milliseconds");
			reasoner = new ReasonerLoader(reasonerName, ont, cSatTimeout, verbose).getReasoner();
		}
		else
			reasoner = new ReasonerLoader(reasonerName, ont, verbose).getReasoner();
		
		Timer t = new Timer(true);
		t.schedule(interrupt, opTimeout);
		
		return testCSAT(reasoner);
	}
	
	
	/**
	 * Test satisfiability of all atomic concepts (and their negation, if applicable) given a reasoner object
	 * @param reasoner	OWL Reasoner
	 * @return Set of SAT results 
	 */
	public Set<SATResult> testCSAT(OWLReasoner reasoner) {
		long start = bean.getCurrentThreadCpuTime();
		Set<SATResult> results = new HashSet<SATResult>();
		
		Set<OWLClassExpression> searchSpace = new HashSet<OWLClassExpression>();
		if(doNegativeTests) searchSpace = defineConceptList(ont);
		else searchSpace.addAll(ont.getClassesInSignature());
		
		OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
		searchSpace.remove(df.getOWLThing());
		searchSpace.remove(df.getOWLNothing());
		
		for(OWLClassExpression c : searchSpace) {
			SATResult r = testSingleCSAT(c, reasoner);
			double t1 = (bean.getCurrentThreadCpuTime()-start)/1000000000.0;
			if(verbose) System.out.println("\t@ t = " + t1 + " seconds");
			if(r != null && verbose) System.out.println(c + " " + r.getSatTestTime());
			
			if(r == null) {
				long indTimeout = reasoner.getTimeOut();
				double indTimeoutSecs = new Long(indTimeout).doubleValue()/1000.0;
				r = new SATResult(c, indTimeoutSecs);
				
				reasoner.dispose();
				reasoner = new ReasonerLoader(reasonerName, ont, indTimeout, verbose).getReasoner();
			}
			r.setElapsedTime(t1);
			results.add(r);
		}
		return results;
	}
	
	
	/**
	 * Test satisfiability of all atomic concepts (and their negation, if applicable) given a reasoner name
	 * @param reasonerName	Reasoner name
	 * @return Set of SAT results
	 */
	public Set<SATResult> testCSAT(String reasonerName) {
		OWLReasoner reasoner = new ReasonerLoader(reasonerName, ont, verbose).getReasoner();
		return testCSAT(reasoner);
	}
	
	
	/**
	 * Test satisfiability of all atomic concepts in isolation, i.e., using a fresh reasoner instance for each SAT test
	 * @param reasonerName	Reasoner name
	 * @return Set of SAT results
	 */
	public Set<SATResult> testCSATinIsolation(String reasonerName, long cSatTimeout) {
		Set<SATResult> results = new HashSet<SATResult>();
		if(verbose && cSatTimeout != 0) System.out.println(" Individual tests timeout: " + cSatTimeout + " milliseconds");
		
		Set<OWLClassExpression> searchSpace = new HashSet<OWLClassExpression>();
		if(doNegativeTests) searchSpace = defineConceptList(ont);
		else searchSpace.addAll(ont.getClassesInSignature());
		
		long start = bean.getCurrentThreadCpuTime();
		for(OWLClassExpression c : searchSpace) {
			OWLReasoner reasoner = null;
			if(cSatTimeout != 0)
				reasoner = new ReasonerLoader(reasonerName, ont, cSatTimeout, false).getReasoner();
			else
				reasoner = new ReasonerLoader(reasonerName, ont, false).getReasoner();
			
			SATResult r = testSingleCSAT(c, reasoner);
			
			double t1 = (bean.getCurrentThreadCpuTime()-start)/1000000000.0;
			if(verbose) System.out.println("t = " + t1 + " seconds");
			if(r != null && verbose) System.out.println("\t" + c + " " + r.getSatTestTime());
			
			if(r == null) {
				long indTimeout = reasoner.getTimeOut();
				double indTimeoutSecs = new Long(indTimeout).doubleValue()/1000.0;
				r = new SATResult(c, indTimeoutSecs);
			}
			r.setElapsedTime(t1);
			results.add(r);
			reasoner.dispose();
		}
		return results;
	}
	
	
	/**
	 * Test satisfiability of all atomic concepts with an overall timeout and in isolation, i.e., using a fresh reasoner instance for each SAT test
	 * @param reasonerName	Reasoner name
	 * @param opTimeout	Timeout in milliseconds
	 * @return Set of SAT results
	 */
	public Set<SATResult> testCSATinIsolationWithOverallTimeout(String reasonerName, long opTimeout, long cSatTimeout) {
		if(verbose) System.out.println(" Overall timeout: " + opTimeout + " milliseconds");
		
		Timer t = new Timer(true);
		t.schedule(interrupt, opTimeout);
		return testCSATinIsolation(reasonerName, cSatTimeout);
	}
	
	
	
	/**
	 * Test satisfiability of a given concept
	 * @param c	Class Expression
	 * @param reasoner	OWL Reasoner
	 * @return SAT result
	 */
	public SATResult testSingleCSAT(OWLClassExpression c, OWLReasoner reasoner) {
		long start = bean.getCurrentThreadCpuTime();
		Boolean sat = null;
		try {
			sat = reasoner.isSatisfiable(c);
		} catch(TimeOutException e) {
			if(verbose) System.out.println(c + " timedout");
		} catch(StackOverflowError | Exception e) {
			if(verbose) System.out.println(c + " error");
		}
		
		long end = bean.getCurrentThreadCpuTime();
		double total = (end-start)/1000000000.0;
		
		if(sat != null) return new SATResult(c, sat, total, reasonerName);
		else return null;
	}
	
	
	/**
	 * Check if ontology is coherent (i.e., does not contain unsatisfiable concepts)
	 * @return true if coherent, false otherwise
	 */
	// TODO
//	public boolean isCoherent(OWLReasoner reasoner) {
//		if(sat == null || unsat == null)
//			testCSAT(reasoner);
//		
//		if(unsat.isEmpty()) return true;
//		else return false;
//	}
	
	
	/**
	 * Serialize the set of SAT results
	 * @param results	Set of SAT results
	 * @throws IOException 
	 */
	public void serializeResults(Set<SATResult> results) throws IOException {
		serializeResults(results, outputDir, "SAT_Test_" + reasonerName + ".csv");
	}
	
	
	/**
	 * Serialize the set of SAT results
	 * @param results	Set of SAT results
	 * @param filename	Output filename
	 * @throws IOException 
	 */
	public void serializeResults(Set<SATResult> results, String outputDir, String filename) throws IOException {
		ResultsSerializer s = new ResultsSerializer(outputDir, filename);
		s.serialize(results, true, true);
	}
	
	
	/**
	 * Interrupt trigger 
	 */
	private TimerTask interrupt = new TimerTask() {
		@Override
		public void run() {
			System.out.println("	Aborted: SAT test exceeded timeout");
			System.exit(0);
		}
	};
	
	
	/**
	 * main
	 * @throws OWLOntologyCreationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, IOException {
		String ontFile = null, reasonerName = null, outputDir = null;
		long opTimeout = 0, cSatTimeout = 0;
		boolean verbose = false, fork = false, ignoreAbox = false;

		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-ont"))		ontFile = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			if(arg.equalsIgnoreCase("-t"))			opTimeout = Long.parseLong(args[++i].trim());
			if(arg.equalsIgnoreCase("-i"))			cSatTimeout = new Double(args[++i].trim()).longValue();
			if(arg.equalsIgnoreCase("-f"))			fork = true;
			if(arg.equalsIgnoreCase("-o"))			outputDir = args[++i].trim();
			if(arg.equalsIgnoreCase("-b"))			ignoreAbox = true;
			if(arg.equalsIgnoreCase("-v"))			verbose = true;
		}

		if(ontFile != null && reasonerName != null) {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			long start = bean.getCurrentThreadCpuTime();
			OWLOntology ont = new OntologyLoader(new File(ontFile), verbose).loadOntology(ignoreAbox);
			SATOntologyTester satTester = new SATOntologyTester(ont, reasonerName, false, outputDir, verbose);
			Set<SATResult> results = null;
			if(fork) {
				if(opTimeout != 0)
					results = satTester.testCSATinIsolationWithOverallTimeout(reasonerName, opTimeout, cSatTimeout);
				else
					results = satTester.testCSATinIsolation(reasonerName, cSatTimeout);
			}
			else {
				if(opTimeout != 0)
					results = satTester.testCSATwithOverallTimeout(reasonerName, opTimeout, cSatTimeout);
				else if(cSatTimeout != 0)
					results = satTester.testCSATwithIndividualTimeout(reasonerName, cSatTimeout);
				else
					results = satTester.testCSAT(reasonerName);
			}
			long end = bean.getCurrentThreadCpuTime();
			double total = (end-start)/1000000000.0;
			if(verbose) System.out.println("SAT testing time: " + total + " seconds");
			
			satTester.serializeResults(results);
		}
		else
			throw new RuntimeException("Error: Minimum parameters are: -ont OntologyFilePath -reasoner ReasonerName.\n" +
					"\tPlease review the usage information via the -h flag.");
	}
}