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
package fi.vm.kapa.sevi.configuration;

import fi.vm.kapa.sevi.ontology.exception.NotFoundException;
import fi.vm.kapa.sevi.ontology.exception.OntologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Exception mapping configurations
 */
public class OntologyExceptionMapper implements ExceptionMapper<OntologyException> {

    private static final Logger LOG = LoggerFactory.getLogger(OntologyExceptionMapper.class);

    @Override
    public Response toResponse(OntologyException exception) {
        if(exception instanceof NotFoundException) {
            return getResponse(NOT_FOUND, exception);
        } else {
            return getResponse(BAD_REQUEST, exception);
        }
    }

    private Response getResponse(Response.Status status, OntologyException exception) {
        LOG.info(exception.getMessage());
        return Response.status(status)
                .entity(new ErrorDTO(exception.getMessage()))
                .build();
    }

    private class ErrorDTO {
        @SuppressWarnings("unused")
        private final String message;

        public ErrorDTO(String message) {
            this.message = message;
        }
    }
}
