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
package uk.ac.manchester.cs.romer.hotspots;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import uk.ac.manchester.cs.romer.reasonertasks.ClassifierResult;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class Hotspot {
	private OWLOntologyManager man;
	private OWLOntology hotspotOnt;
	private Set<OWLAxiom> hotspot;
	private ClassifierResult clResult;
	private Double prepTime, classificationTime;
	private String hsPath;
	private OWLClass seed;
	private int candidateNr;
	
	/**
	 * Constructor
	 * @param man	OWLOntologyManager
	 * @param hotspot	Hotspot axioms
	 * @param prepTime	Hotspot preparation time (in seconds)
	 * @param seed	Seed concept
	 */
	public Hotspot(OWLOntologyManager man, Set<OWLAxiom> hotspot, Double prepTime, OWLClass seed, int candidateNr) {
		this.man = man;
		this.hotspot = hotspot;
		this.prepTime = prepTime;
		this.seed = seed;
		this.candidateNr = candidateNr;
	}
	
	
	/**
	 * Set path of hotspot file in file system
	 * @param path	Absolute path to hotspot file
	 */
	public void setPath(String path) {
		hsPath = path;
	}
	
	
	/**
	 * Get absolute path to hotspot file
	 * @return Absolute path to hotspot file
	 */
	public String getPath() {
		return hsPath;
	}
	
	
	/**
	 * Get time to prepare candidate hot spot (incl. module extraction, if applicable), in seconds
	 * @return Time to prepare candidate hot spot (in seconds)
	 */
	public Double getPreparationTime() {
		return prepTime;
	}
	
	
	/**
	 * Get seed concept
	 * @return Seed concept
	 */
	public OWLClass getSeed() {
		return seed;
	}
	
	
	/**
	 * Get seed concept name
	 * @return Seed concept name
	 */
	public String getSeedName() {
		return new SimpleShortFormProvider().getShortForm(seed);
	}
	
	
	/**
	 * Get number of axioms in hot spot
	 * @return Number of axioms in hot spot
	 */
	public Integer getSize() {
		return hotspot.size();
	}
	
	
	/**
	 * Get hotspot axioms
	 * @return Hotspot axioms
	 */
	public Set<OWLAxiom> getAxioms() {
		return hotspot;
	}
	
	
	/**
	 * Get signature of hotspot
	 * @return Set of terms in hotspot's signature
	 */
	public Set<OWLEntity> getSignature() {
		return getHotspotAsOntology().getSignature();
	}
	
	
	/**
	 * Get hotspot candidate number
	 * @return Hotspot candidate number
	 */
	public int getCandidateNr() {
		return candidateNr;
	}
	
	
	/**
	 * Get hotspot as an OWLOntology
	 * @return Hotspot as an OWL Ontology
	 * @throws OWLOntologyCreationException 
	 */
	public OWLOntology getHotspotAsOntology() {
		if(hotspotOnt == null) {
			try { hotspotOnt = man.createOntology(hotspot); }
			catch (OWLOntologyCreationException e) { e.printStackTrace(); }
		}
		return hotspotOnt;
	}
	
	
	/**
	 * Get classification time of hotspot (in milliseconds)
	 * @return Classification time of hotspot (in milliseconds)
	 */
	public Double getClassificationTime() {
		return classificationTime;
	}
	
	
	/**
	 * Set classification time of hotspot (in milliseconds)
	 * @param time	Classification time of hotspot (in milliseconds)
	 */
	public void setClassificationTime(Double time) {
		classificationTime = time;
	}
	
	
	/**
	 * Get classification results of the hotspot
	 * @return Hotspot classification results
	 */
	public ClassifierResult getClassificationResults() {
		return clResult;
	}
	
	
	/**
	 * Attach hotspot's classification results
	 * @param r	Classifier result
	 */
	public void attachClassifierResults(ClassifierResult r) {
		this.clResult = r;
		setClassificationTime(clResult.getReasoningTaskTime());
	}
}
