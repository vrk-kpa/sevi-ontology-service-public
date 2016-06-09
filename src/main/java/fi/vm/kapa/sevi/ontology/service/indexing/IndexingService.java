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

import fi.vm.kapa.sevi.ontology.dto.ConceptDTO;
import fi.vm.kapa.sevi.ontology.dto.ViewDTO;
import fi.vm.kapa.sevi.service.commons.JMSUtil;
import fi.vm.kapa.sevi.service.commons.QueueConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IndexingService {
    
    private static final Logger LOG = LoggerFactory.getLogger(IndexingService.class);
    
    @Autowired
    private JMSUtil jmsUtil;

    /**
     * We will index all the concepts as JSON in the searchable ElasticSearch database.
     * @param concepts All concepts to index in ElasticSearch
     * @return
     */
    public boolean reIndexConcepts(Stream<ConceptDTO> concepts) {
        LOG.info("Indexing concepts");
        final long indexTime = System.currentTimeMillis();

        try {
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPool.submit(() ->
                    concepts.parallel().forEach((object) -> jmsUtil.sendForIndexing(object, indexTime, QueueConstants.QUEUE_ONTOLOGY_CHANGED))
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldn't parallel index", e);
        }
        return true;
    }

    public void reIndexTopLevelConcepts(Stream<Object> concepts) {
        final long indexTime = System.currentTimeMillis();

        List<Object> targets = concepts.collect(Collectors.toList());
        jmsUtil.sendObjectsForIndexing(targets, indexTime, QueueConstants.QUEUE_ONTOLOGY_TOPLEVELS_CHANGED);
    }

    public void reIndexViews(Collection<ViewDTO> views) {
        final long indexTime = System.currentTimeMillis();

        jmsUtil.sendObjectsForIndexing(new ArrayList<>(views), indexTime, QueueConstants.QUEUE_ONTOLOGY_VIEWS_CHANGED);
    }

}
