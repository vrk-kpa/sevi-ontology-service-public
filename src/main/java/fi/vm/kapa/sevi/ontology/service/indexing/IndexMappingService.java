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
package fi.vm.kapa.sevi.ontology.service.indexing;

import fi.vm.kapa.sevi.service.commons.IndexMappingServiceBase;
import fi.vm.kapa.sevi.service.commons.QueueConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

@Service
public class IndexMappingService extends IndexMappingServiceBase {

    private static final Logger LOG = LoggerFactory.getLogger(IndexMappingService.class);

    private static final String DEFAULT_MAPPING_CLASSPATH_LOCATION = "/mapping/{0}-mapping.json";
    private static final String DEFAULT_MAPPING_FILE = MessageFormat.format(DEFAULT_MAPPING_CLASSPATH_LOCATION, "ontology");

    public boolean putMappings(String mappingJson) {
        return putMappings(mappingJson, DEFAULT_MAPPING_FILE, QueueConstants.QUEUE_ONTOLOGY_UPDATE_MAPPINGS);
    }

    @Override
    public Logger getLog() {
        return LOG;
    }
}
