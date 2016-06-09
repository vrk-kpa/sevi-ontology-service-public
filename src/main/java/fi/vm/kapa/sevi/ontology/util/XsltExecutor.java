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
package fi.vm.kapa.sevi.ontology.util;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class XsltExecutor {
    /**
     * Runs the specified XSLT transformation for an XML resource given as
     *
     * @param input     the xml resource as an InputStream
     * @param xsltFile  name of the xslt file
     *                  file must be available as a resource on the classpath
     *
     * @return          an InputStream with the result of the transformation for further processing
     *
     */
    public InputStream executeTransformation(InputStream input, String xsltFile) throws TransformerException {
        StreamSource sourceXml = new StreamSource(input);
        StreamSource transformation = new StreamSource(getResourceAsStream(xsltFile));

        return doTransform(sourceXml, transformation);
    }

    private InputStream getResourceAsStream(String xsltFile) {
        return XsltExecutor.class.getClassLoader().getResourceAsStream(xsltFile);
    }

    private ByteArrayInputStream doTransform(StreamSource sourceXml, StreamSource transformation) throws TransformerException {
        Transformer transformer = getTransformer(transformation);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(out);
        transformer.transform(sourceXml, result);

        return getResultStream(out);
    }

    private ByteArrayInputStream getResultStream(ByteArrayOutputStream out) {
        return new ByteArrayInputStream(out.toByteArray());
    }

    private Transformer getTransformer(StreamSource transformation) throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        return factory.newTransformer(transformation);
    }

}
