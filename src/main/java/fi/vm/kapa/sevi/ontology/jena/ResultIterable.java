package fi.vm.kapa.sevi.ontology.jena;

import java.util.Iterator;
import org.apache.jena.query.*;

import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;

/**
 * Adapting the Jena result set into an iterable of ConceptDTOs.
 */
public class ResultIterable implements Iterable<ConceptDTO> {
    private ResultSet results;

    public ResultIterable(ResultSet results) {
        this.results = results;
    }
    
    @Override
    public Iterator<ConceptDTO> iterator() {
        return new ResultIterator(results);
    }
}
