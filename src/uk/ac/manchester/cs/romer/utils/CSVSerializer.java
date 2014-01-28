package uk.ac.manchester.cs.romer.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.metrics.GCICount;
import org.semanticweb.owlapi.metrics.HiddenGCICount;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.semanticweb.owlapi.util.DLExpressivityChecker;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class CSVSerializer {
	private FileWriter out;
	private String headers = "", row = "";
	private boolean includeHeaders;
	
	/**
	 * Constructor
	 * @param outputDir	Output directory
	 * @param filename	CSV file name
	 */
	public CSVSerializer(String outputDir, String filename, boolean includeHeaders) {
		this.includeHeaders = includeHeaders;
		if(!outputDir.endsWith(File.separator))
			outputDir += File.separator;
		
		new File(outputDir).mkdirs();
		try {
			out = new FileWriter(new File(outputDir + filename + ".csv"), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Set the standard row of information about a given ontology: ontology name, expressivity, nr. logical axioms,
	 * nr. of GCIs, signature size, nr. of classes, properties, individuals, datatypes, and ontology profile
	 */
	public void setStandardHeader() {
		headers = "Ontology,Parse Time,Expressivity,Nr. Logical Axioms,Nr. GCIs,Nr. Hidden GCIs,Nr. Entities,Nr. Classes," +
				"Nr. Object Properties,Nr. Data Properties,Nr. Individuals,Nr. Datatypes,OWL 2,OWL2 DL,OWL2 EL," +
				"OWL2 QL,OWL2 RL,";
	}
	
	
	/**
	 * Set the standard row of information about a given ontology: ontology name, expressivity, nr. logical axioms,
	 * nr. of GCIs, signature size, nr. of classes, properties, individuals, datatypes, and ontology profile
	 * @param ont	OWLOntology
	 * @param ontName	Ontology file name
	 * @param parseTime	Ontology parsing time
	 */
	public void setStandardRow(OWLOntology ont, String ontName, double parseTime) {
		injectDeclarations(ont);
		if(includeHeaders) setStandardHeader();
	
		GCICount count = new GCICount(ont.getOWLOntologyManager());
		count.setOntology(ont);
		int nrGcis = count.getValue();
		
		HiddenGCICount hcount = new HiddenGCICount(ont.getOWLOntologyManager());
		hcount.setOntology(ont);
		int nrHiddenGcis = hcount.getValue();

		row = ontName + "," + parseTime + "," + checkExpressivity(ont) + "," + ont.getLogicalAxiomCount() + "," + nrGcis + ","
				+ nrHiddenGcis + "," + ont.getSignature().size() + "," + ont.getClassesInSignature().size() + ","
				+ ont.getObjectPropertiesInSignature().size() + "," + ont.getDataPropertiesInSignature().size() + ","
				+ ont.getIndividualsInSignature().size() + "," + ont.getDatatypesInSignature().size() + "," + isOWL2(ont) + ","
				+ isOWL2DL(ont) + "," + isOWL2EL(ont) + "," + isOWL2QL(ont) + "," + isOWL2RL(ont) + ",";
	}
	
	
	/**
	 * Append a header and row value to the output file
	 * @param headerValue
	 * @param rowValue
	 */
	public void appendToCsv(String headerValue, String rowValue) {
		if(includeHeaders)
			appendToHeader(headerValue);
		
		appendToRow(rowValue);
	}
	
	
	/**
	 * @param s	Header value
	 */
	public void appendToHeader(String s) {
		headers += s + ",";
	}
	
	
	/**
	 * @param s	Row value
	 */
	public void appendToRow(String s) {
		row += s + ",";
	}
	
	
	/**
	 * Finalize header by adding a new line and flushing to output Writer
	 * @throws IOException
	 */
	public void finalizeHeader() throws IOException {
		out.append(headers + "\n");
		flush();
	}
	
	
	/**
	 * Finalize row by adding a new line and flushing to output Writer
	 * @throws IOException
	 */
	public void finalizeRow() throws IOException {
		out.append(row + "\n");
		flush();
	}
	
	
	/**
	 * Flush output Writer
	 * @throws IOException
	 */
	public void flush() throws IOException {
		out.flush();
	}
	
	
	/**
	 * Finalize both header and row of CSV file, and close the output stream
	 */
	public void finalize() throws IOException {
		if(includeHeaders) finalizeHeader();
		finalizeRow();
		out.close();
	}
	
	
	/**
	 * Check if ontology is in OWL2
	 * @param ont	OWLOntology
	 * @return true if in OWL2, false otherwise
	 */
	public boolean isOWL2(OWLOntology ont) {
		return new OWL2Profile().checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in OWL2 DL profile
	 * @param ont	OWLOntology
	 * @return true if in OWL2 DL profile, false otherwise
	 */
	public boolean isOWL2DL(OWLOntology ont) {
		return new OWL2DLProfile().checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in OWL2 EL profile
	 * @param ont	OWLOntology
	 * @return true if in OWL2 EL profile, false otherwise
	 */
	public boolean isOWL2EL(OWLOntology ont) {
		return new OWL2ELProfile().checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in OWL2 RL profile
	 * @param ont	OWLOntology
	 * @return true if in OWL2 RL profile, false otherwise
	 */
	public boolean isOWL2RL(OWLOntology ont) {
		return new OWL2RLProfile().checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in OWL2 QL profile
	 * @param ont	OWLOntology
	 * @return true if in OWL2 QL profile, false otherwise
	 */
	public boolean isOWL2QL(OWLOntology ont) {
		return new OWL2QLProfile().checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check expressivity of given ontology
	 * @param ont	OWLOntology
	 * @return Expressivity of the ontology as a string
	 */
	private String checkExpressivity(OWLOntology ont) {
		return new DLExpressivityChecker(ont.getImportsClosure()).getDescriptionLogicName();
	}
	
	
	/**
	 * Inject missing entity declarations (that might affect OWL 2 profile checkers' results)
	 * @param ont	OWL ontology
	 */
	private void injectDeclarations(OWLOntology ont) {
		OWLOntologyManager man = ont.getOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		List<AddAxiom> adds = new ArrayList<AddAxiom>();
		for(OWLEntity e : ont.getSignature()) {
			if(ont.getDeclarationAxioms(e).isEmpty())
				adds.add(new AddAxiom(ont, df.getOWLDeclarationAxiom(e)));
		}
		man.applyChanges(adds);
	}
}