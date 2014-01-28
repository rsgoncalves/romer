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

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.util.InferredClassAxiomGenerator;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class CustomInferredSubClassAxiomGenerator extends InferredClassAxiomGenerator<OWLSubClassOfAxiom> {
	private boolean directOnly;
	
	/**
	 * Constructor
	 * @param directOnly	true if asserted atomic subsumptions should be included, false otherwise 
	 */
	public CustomInferredSubClassAxiomGenerator(boolean directOnly) {
		this.directOnly = directOnly;
	}
	
	
	@Override
	protected void addAxioms(OWLClass entity, OWLReasoner reasoner, OWLDataFactory dataFactory, Set<OWLSubClassOfAxiom> result) throws TimeOutException {
		if(reasoner.isSatisfiable(entity)) {
			for(OWLClass sup : reasoner.getSuperClasses(entity, directOnly).getFlattened()) {
				result.add(dataFactory.getOWLSubClassOfAxiom(entity, sup));
			}
		} 
		else
			result.add(dataFactory.getOWLSubClassOfAxiom(entity, dataFactory.getOWLNothing()));
	}
	
	
	public String getLabel() {
        return "Subclasses";
    }
}
