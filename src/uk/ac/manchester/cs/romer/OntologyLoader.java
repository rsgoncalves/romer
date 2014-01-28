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
package uk.ac.manchester.cs.romer;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class OntologyLoader {
	private File f;
	private ThreadMXBean bean;
	private double parseTime;
	private boolean verbose;
	
	/**
	 * Constructor
	 * @param f	Ontology file
	 */
	public OntologyLoader(File f, boolean verbose) {
		this.f = f;
		this.verbose = verbose;
		this.bean = ManagementFactory.getThreadMXBean();
	}
	
	
	/**
	 * Load Ontology
	 * @return OWLOntology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology loadOntology() throws OWLOntologyCreationException {
		if(verbose)
			System.out.println(" Input: " + f.getAbsolutePath());
		
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setLoadAnnotationAxioms(false);
//		config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		
		IRIDocumentSource iriSrc = new IRIDocumentSource(IRI.create("file:" + f.getAbsolutePath()));
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = null;
		
		long start = bean.getCurrentThreadCpuTime();
		
		ont = man.loadOntologyFromOntologyDocument(iriSrc, config);
		
		long end = bean.getCurrentThreadCpuTime();
		parseTime = (end-start)/1000000000.0;
		
		Set<OWLClass> classes = ont.getClassesInSignature();
		classes.remove(man.getOWLDataFactory().getOWLThing());
		classes.remove(man.getOWLDataFactory().getOWLNothing());
		
		if(verbose) {
			System.out.println(" Loaded ontology: " + f.getName() + " (parse time: " + parseTime + " seconds)");
			System.out.println("   Nr. logical axioms: " + ont.getLogicalAxiomCount());
			System.out.println("   Nr. classes: " + classes.size());
		}
		
		return ont;
	}
	
	
	/**
	 * Load ontology with option to ignore Abox
	 * @param ignoreAbox	Ignore Abox axioms
	 */
	public OWLOntology loadOntology(boolean ignoreAbox) throws OWLOntologyCreationException {
		OWLOntology ont = loadOntology();
		if(ignoreAbox) 
			removeAboxAxioms(ont);
		return ont;
	}
	
	
	/**
	 * Remove ABox axioms from given ontology
	 * @param ont	OWLOntology
	 */
	public void removeAboxAxioms(OWLOntology ont) {
		ont.getOWLOntologyManager().removeAxioms(ont, ont.getABoxAxioms(true));
	}
	
	
	/**
	 * Get ontology loading time (in seconds)
	 * @return Ontology load time
	 */
	public double getLoadTime() {
		return parseTime;
	}
}
