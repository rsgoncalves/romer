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

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class RealizerResult extends EntailmentGeneratorResult {

	/**
	 * Constructor
	 * @param results	Reasoning task results
	 * @param time	Task time
	 * @param errorMsg	Error message (if applicable)
	 */
	public RealizerResult(Set<OWLAxiom> results, double time, String errorMsg, boolean isConsistent) {
		super(results, time, errorMsg, isConsistent);
	}
}
