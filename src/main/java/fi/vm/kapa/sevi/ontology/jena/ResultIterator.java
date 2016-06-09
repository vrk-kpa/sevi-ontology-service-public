package fi.vm.kapa.sevi.ontology.jena;

import java.util.Iterator;
import org.apache.jena.query.*;

import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;

/**
 * Adapting the Jena result set into an iterator of ConceptDTOs.
 */
public class ResultIterator implements Iterator<ConceptDTO> {
    private ResultSet results;

    public ResultIterator(ResultSet results) {
        this.results = results;
    }

    @Override
    public boolean hasNext() {
        return results.hasNext();
    }

    @Override
    public ConceptDTO next() {
        QuerySolution solution = results.next();
        // Note: Some queries signify empty results with one item with "" as
        // URI.
        String uri = solution.get("?uri") != null ? solution.get("?uri").toString() : "";
        String label = solution.get("?label") != null ? solution.get("?label").toString() : "";
        String notation = solution.get("?notation") != null ? solution.get("?notation").toString() : "";
        String scheme = solution.get("?scheme") != null ? solution.get("?scheme").toString() : "";
        return new ConceptDTO(uri, label, notation, scheme);
    }
}
