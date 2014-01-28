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
package uk.ac.manchester.cs.romer.approximation;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ELApproximation {
	private Set<OWLAxiom> axioms;
	private OWLOntologyManager man;
	private OWLOntology ont;
	
	
	/**
	 * Constructor 1
	 * @param axioms	Set of axioms to be approximated to EL
	 */
	public ELApproximation(Set<OWLAxiom> axioms) {
		this.axioms = axioms;
		this.man = OWLManager.createOWLOntologyManager();
		try {
			this.ont = man.createOntology(axioms);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Constructor 2
	 * @param ont	OWLOntology to be approximated
	 */
	public ELApproximation(OWLOntology ont) {
		this.ont = ont;
		this.man = ont.getOWLOntologyManager();
		this.axioms = ont.getAxioms();
	}
	
	
	/**
	 * Get an EL approximated version of the given ontology
	 * @param type	EL approximation type
	 * @return	EL approximation of input as an OWLOntology
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology getELApproximationAsOntology(ELApproximationType type) throws OWLOntologyCreationException {
		return man.createOntology(getELApproximation(type));
	}
	
	
	/**
	 * Get an EL approximated version of the given ontology 
	 * @param type	EL approximation type
	 * @return EL approximation of input
	 * @throws OWLOntologyCreationException 
	 */
	public Set<OWLAxiom> getELApproximation(ELApproximationType type) {
		Set<OWLAxiom> result = new HashSet<OWLAxiom>();
		if(type.equals(ELApproximationType.NAIVE))
			result = getNaiveELApproximation();
		else if(type.equals(ELApproximationType.TrOWL)) 
			result = getTrOWLELApproximation();
		else if(type.equals(ELApproximationType.TERM))
			result = getELTerminologyApproximation();
		return result;
	}
	
	
	/**
	 * Get a naive EL approximation where all non-EL axioms are removed
	 * @return Set of EL axioms
	 */
	public Set<OWLAxiom> getNaiveELApproximation() {
		Set<OWLAxiom> output = new HashSet<OWLAxiom>(axioms);
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		OWL2ELProfile elprofile = new OWL2ELProfile();
		for(OWLProfileViolation violation : elprofile.checkOntology(ont).getViolations()) {
			toRemove.add(violation.getAxiom());
		}
		output.removeAll(toRemove);
		return output;
	}
	
	
	/**
	 * Get an EL approximation based on TrOWL's EL-reduction algorithm
	 * @return Set of EL axioms
	 * @throws OWLOntologyCreationException 
	 */
	public Set<OWLAxiom> getTrOWLELApproximation() {
		TrOWLELApproximation trowl = new TrOWLELApproximation();
		Set<OWLAxiom> output = null;
		try {
			output = trowl.getApproximatedOntologyAxioms(ont);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return output;
	}

	
	/**
	 * 
	 * @return Set of EL terminological axioms
	 */
	public Set<OWLAxiom> getELTerminologyApproximation() {
		Set<OWLAxiom> output = new HashSet<OWLAxiom>();
		// TODO
		return output;
	}
	
	
	/**
	 * EL approximation type
	 */
	public enum ELApproximationType {
		NAIVE	("Remove all non-EL axioms"),
		TrOWL	("Use TrOWL's EL reduction algorithm"),
		TERM	("Approximate into an EL terminology");
		
		String name;
		ELApproximationType(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}
}
