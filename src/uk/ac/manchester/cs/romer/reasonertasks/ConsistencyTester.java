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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import uk.ac.manchester.cs.romer.OntologyLoader;
import uk.ac.manchester.cs.romer.ReasonerLoader;

/**
 * @author Rafael S. Goncalves
 * Information Management Group (IMG)
 * School of Computer Science
 * University of Manchester
 */
public class ConsistencyTester {
	private OWLReasoner reasoner;
	private ThreadMXBean bean;
	
	/**
	 * Constructor 1
	 * @param reasoner	OWLReasoner
	 */
	public ConsistencyTester(OWLReasoner reasoner) {
		this.reasoner = reasoner;
		this.bean = ManagementFactory.getThreadMXBean();
	}
	
	
	/**
	 * Constructor 2
	 * @param ont	OWLOntology
	 * @param reasonerName	Name of reasoner to be used
	 */
	public ConsistencyTester(OWLOntology ont, String reasonerName, boolean verbose) {
		this.reasoner = new ReasonerLoader(reasonerName, ont, verbose).getReasoner();
		this.bean = ManagementFactory.getThreadMXBean();
	}
	
	
	/**
	 * Check ontology consistency
	 * @return ConsistencyResult
	 */
	public ConsistencyResult isConsistent() {
		long start = bean.getCurrentThreadCpuTime();
		
		boolean cons = reasoner.isConsistent();
		
		long end = bean.getCurrentThreadCpuTime();
		double total = (end-start)/1000000000.0;
		
		return new ConsistencyResult(cons, total);
	}
	
	
	/**
	 * Check ontology consistency with a timeout
	 * @param timeout	Timeout in milliseconds
	 * @return ConsistencyResult
	 */
	public ConsistencyResult isConsistent(long timeout) {
		Timer t = new Timer(true);
		t.schedule(interrupt, timeout);
		return isConsistent();
	}
	
	
	/**
	 * Interrupt trigger 
	 */
	private TimerTask interrupt = new TimerTask() {
		@Override
		public void run() {
			System.out.println("	Aborted: Consistency check exceeded timeout");
			System.exit(0);
		}
	};
	
	
	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException {
		System.out.println("Executing consistency tester...");
		String ontFile = "", reasonerName = "";
		long timeout = 0;
		boolean verbose = false;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-ont"))		ontFile = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			if(arg.equalsIgnoreCase("-t"))			timeout = Long.parseLong(args[++i].trim());
			if(arg.equalsIgnoreCase("-v"))			verbose = true;
		}

		if(ontFile != null && reasonerName != null) {
			ConsistencyTester tester = new ConsistencyTester(new OntologyLoader(new File(ontFile), verbose).loadOntology(), reasonerName, verbose);
			ConsistencyResult r = null;
			if(timeout != 0)
				r = tester.isConsistent(timeout);
			else
				r = tester.isConsistent();
			
			if(r != null) {
				boolean isConsistent = r.isConsistent();
				if(isConsistent)
					System.out.print(" Ontology is consistent");
				else
					System.out.print(" Ontology is inconsistent");

				System.out.println(" (consistency checking time: " + r.getTime() + " seconds)");
			}
		}
	}
}
