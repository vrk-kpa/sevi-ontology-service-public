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
import java.util.ArrayList;
import java.util.List;

import fi.vm.kapa.sevi.service.commons.ConceptType;

public class ViewDTO extends AbstractDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<ViewDTO> children = new ArrayList<>();

    /**
     * A dummy constructor or jackson may stumble with mapping :(
     */
    public ViewDTO() {
        super();
    }

    public ViewDTO(String id, String label, ConceptType conceptType) {
        this(id, label, "", conceptType);
    }

    public ViewDTO(String id, String label, String notation, ConceptType conceptType) {
        super(id, label, notation, conceptType);
    }

    public ViewDTO(ConceptDTO concept) {
        super(concept);
    }

    public List<ViewDTO> getChildren() {
        return children;
    }

    public void setChildren(List<ViewDTO> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "ViewDTO [children=" + children + ", getId()=" + getId() + ", getLabel()=" + getLabel()
                + ", getFinnish()=" + getFinnish() + ", getNotation()=" + getNotation() + ", getConceptType()="
                + getConceptType() + "]";
    }
}
