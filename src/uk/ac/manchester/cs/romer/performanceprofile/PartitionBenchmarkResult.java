package uk.ac.manchester.cs.romer.performanceprofile;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 * <p>
 * <code>PartitionBenchmarkResult</code> represents the result of benchmarking an ontology partition.
 * </p>
 */
public class PartitionBenchmarkResult {
	private int partitionNr;
	private String id, path;
	private OWLOntology ont;
	private Double clTime;
	
	
	/**
	 * Constructor
	 * @param partitionNr	Number of partition
	 * @param id	Unique identifier of the partition
	 * @param path	File path of the ontology file
	 * @param ont	Partition's OWLOntology object
	 * @param clTime	Classification time of partition
	 */
	public PartitionBenchmarkResult(int partitionNr, String id, String path, OWLOntology ont, Double clTime) {
		this.partitionNr = partitionNr;
		this.id = id;
		this.path = path;
		this.ont = ont;
		this.clTime = clTime;
	}
	
	
	/**
	 * Get partition number 
	 * @return Partition number
	 */
	public int getNumber() {
		return partitionNr;
	}
	
	
	/**
	 * Get unique ID of partition
	 * @return Unique partition ID
 	 */
	public String getID() {
		return id;
	}
	
	
	/**
	 * Get file path to serialized partition
	 * @return File path to serialized partition
	 */
	public String getFilePath() {
		return path;
	}
	
	
	/**
	 * Get partition ontology
	 * @return Partition ontology
	 */
	public OWLOntology getOntology() {
		return ont;
	}

	
	/**
	 * Get classification time of partition
	 * @return Classification time of partition
	 */
	public Double getClassificationTime() {
		return clTime;
	}
	
	
	/**
	 * Get axioms in partition ontology
	 * @return Axioms in partition ontology
	 */
	public Set<OWLAxiom> getAxioms() {
		return ont.getAxioms();
	}
	
	
	/**
	 * Get size of partition (number of logical axioms)
	 * @return Number of logical axioms in partition
	 */
	public int getSize() {
		return ont.getLogicalAxiomCount();
	}
}
