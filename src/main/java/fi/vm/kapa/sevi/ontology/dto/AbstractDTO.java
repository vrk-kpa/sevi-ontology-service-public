/**
 * The MIT License
 * Copyright (c) 2015 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.sevi.ontology.dto;

import java.io.Serializable;

import fi.vm.kapa.sevi.service.commons.ConceptType;

public class AbstractDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;

    private String notation;
    private String label;
    private String finnish;
    private String swedish;
    private String english;
    private ConceptType conceptType;

    public AbstractDTO() {
        // A dummy constructor or jackson may stumble with mapping :(
    }

    public AbstractDTO(String id, String label, ConceptType conceptType) {
        this(id, label, "", conceptType);
    }

    public AbstractDTO(String id, String label, String notation, ConceptType conceptType) {
        this.id = id;
        this.label = label;
        this.finnish = label;
        this.swedish = label;
        this.english = label;
        this.notation = notation;
        this.conceptType = conceptType;
    }

    public AbstractDTO(ConceptDTO concept) {
        this(concept.getId(), concept.getLabel(), concept.getNotation(), concept.getConceptType());
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFinnish() {
        return finnish;
    }

    public void setFinnish(String finnish) {
        this.finnish = finnish;
    }

    public String getNotation() {
        return notation;
    }

    public void setNotation(String notation) {
        this.notation = notation;
    }

    public ConceptType getConceptType() {
        return conceptType;
    }

    public void setConceptType(ConceptType conceptType) {
        this.conceptType = conceptType;
    }

    @Override
    public String toString() {
        return "AbstractDTO [id=" + id + ", notation=" + notation + ", label=" + label + ", finnish=" + finnish
                + ", swedish=" + swedish + ", english=" + english + ", conceptType=" + conceptType + "]";
    }
}
