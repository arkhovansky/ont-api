/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ru.avicomp.owlapi;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is an original OWLOntologyFactory implementation which is usually used if syntax format of source can not be handle by Apache Jena.
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 10/04/15
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
 */
@SuppressWarnings({"NullableProblems", "WeakerAccess"})
public class OWLOntologyFactoryImpl implements OWLOntologyFactory {

    private final Set<String> parsableSchemes = Sets.newHashSet("http", "https", "file", "ftp");
    private final OWLOntologyBuilder ontologyBuilder;

    /**
     * @param ontologyBuilder ontology builder
     */
    public OWLOntologyFactoryImpl(OWLOntologyBuilder ontologyBuilder) {
        this.ontologyBuilder = Objects.requireNonNull(ontologyBuilder);
    }

    /**
     * Selects parsers by MIME type and format of the input source, if known.
     * If format and MIME type are not known or not matched by any parser, returns all known parsers.
     *
     * @param documentSource document source
     * @param parsers        parsers
     * @return selected parsers
     */
    private static PriorityCollection<OWLParserFactory> getParsers(OWLOntologyDocumentSource documentSource, PriorityCollection<OWLParserFactory> parsers) {
        if (parsers.isEmpty()) {
            return parsers;
        }
        Optional<OWLDocumentFormat> format = documentSource.getFormat();
        Optional<String> mimeType = documentSource.getMIMEType();
        if (!format.isPresent() && !mimeType.isPresent()) {
            return parsers;
        }
        PriorityCollection<OWLParserFactory> candidateParsers = parsers;
        if (format.isPresent()) {
            candidateParsers = getParsersByFormat(format.get(), parsers);
        }
        if (candidateParsers.isEmpty() && mimeType.isPresent()) {
            candidateParsers = getParserCandidatesByMIME(mimeType.get(), parsers);
        }
        if (candidateParsers.isEmpty()) {
            return parsers;
        }
        return candidateParsers;
    }

    /**
     * Use the format to select a sublist of parsers.
     *
     * @param format  document format
     * @param parsers parsers
     * @return candidate parsers
     */
    private static PriorityCollection<OWLParserFactory> getParsersByFormat(OWLDocumentFormat format, PriorityCollection<OWLParserFactory> parsers) {
        PriorityCollection<OWLParserFactory> candidateParsers = new PriorityCollection<>(PriorityCollectionSorting.NEVER);
        for (OWLParserFactory parser : parsers) {
            if (parser.getSupportedFormat().getKey().equals(format.getKey())) {
                candidateParsers.add(parser);
            }
        }
        return candidateParsers;
    }

    /**
     * Use the MIME type it to select a sublist of parsers.
     *
     * @param mimeType MIME type
     * @param parsers  parsers
     * @return candidate parsers
     */
    private static PriorityCollection<OWLParserFactory> getParserCandidatesByMIME(String mimeType, PriorityCollection<OWLParserFactory> parsers) {
        return parsers.getByMIMEType(mimeType);
    }

    @Override
    public boolean canCreateFromDocumentIRI(IRI documentIRI) {
        return true;
    }

    @Override
    public boolean canAttemptLoading(OWLOntologyDocumentSource source) {
        return !source.hasAlredyFailedOnStreams() || !source.hasAlredyFailedOnIRIResolution() && parsableSchemes.contains(source.getDocumentIRI().getScheme());
    }

    @Override
    public OWLOntology createOWLOntology(OWLOntologyManager manager, OWLOntologyID ontologyID, IRI documentIRI, OWLOntologyCreationHandler handler) {
        OWLOntology ont = ontologyBuilder.createOWLOntology(manager, ontologyID);
        handler.ontologyCreated(ont);
        handler.setOntologyFormat(ont, new RDFXMLDocumentFormat());
        return ont;
    }

    @Override
    public OWLOntology loadOWLOntology(OWLOntologyManager manager,
                                       OWLOntologyDocumentSource documentSource,
                                       OWLOntologyCreationHandler handler,
                                       OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        // Attempt to parse the ontology by looping through the parsers.
        // If the ontology is parsed successfully then we break out and return the ontology.
        // I think that this is more reliable than selecting a parser based on a file extension for example
        // (perhaps the parser list could be ordered based on most likely parser,
        // which could be determined by file extension).
        Map<OWLParser, OWLParserException> exceptions = new LinkedHashMap<>();
        // Call the super method to create the ontology - this is needed,
        // because we throw an exception if someone tries to create an ontology directly
        OWLOntology existingOntology = null;
        IRI iri = documentSource.getDocumentIRI();
        if (manager.contains(iri)) {
            existingOntology = manager.getOntology(iri);
        }
        OWLOntologyID ontologyID = new OWLOntologyID();
        OWLOntology ont = createOWLOntology(manager, ontologyID, documentSource.getDocumentIRI(), handler);
        // Now parse the input into the empty ontology that we created select a parser
        // if the input source has format information and MIME information
        Set<String> bannedParsers = Sets.newHashSet(configuration.getBannedParsers().split(" "));
        PriorityCollection<OWLParserFactory> parsers = getParsers(documentSource, manager.getOntologyParsers());
        // use the selection of parsers to set the accept headers explicitly, including weights
        if (documentSource.getAcceptHeaders().isPresent()) {
            documentSource.setAcceptHeaders(AcceptHeaderBuilder.headersFromParsers(parsers));
        }
        for (OWLParserFactory parserFactory : parsers) {
            if (!bannedParsers.contains(parserFactory.getClass().getName())) {
                OWLParser parser = parserFactory.createParser();
                try {
                    if (existingOntology == null && !ont.isEmpty()) {
                        // Junk from a previous parse. We should clear the ont
                        manager.removeOntology(ont);
                        ont = createOWLOntology(manager, ontologyID, documentSource.getDocumentIRI(), handler);
                    }
                    OWLDocumentFormat format = parser.parse(documentSource, ont, configuration);
                    handler.setOntologyFormat(ont, format);
                    return ont;
                } catch (UnloadableImportException e) {
                    // If an import cannot be located, all parsers will fail.
                    // Again, terminate early
                    // First clean up
                    manager.removeOntology(ont);
                    throw e;
                } catch (OWLParserException e) {
                    if (e.getCause() instanceof IOException || e.getCause() instanceof OWLOntologyInputSourceException) {
                        // For input/output exceptions, we assume that it means
                        // the source cannot be read regardless of the parsers,
                        // so we stop early/
                        // First clean up
                        manager.removeOntology(ont);
                        throw new OWLOntologyCreationIOException(e.getCause());
                    }
                    // Record this attempts and continue trying to parse.
                    exceptions.put(parser, e);
                } catch (RuntimeException e) {
                    // Clean up and rethrow
                    exceptions.put(parser, new OWLParserException(e));
                    manager.removeOntology(ont);
                    throw e;
                }
            }
        }
        if (existingOntology == null) {
            manager.removeOntology(ont);
        }
        // We haven't found a parser that could parse the ontology properly.
        // Throw an exception whose message contains the stack traces from all of the parsers that we have tried.
        throw new UnparsableOntologyException(documentSource.getDocumentIRI(), exceptions, configuration);
    }

    /**
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/AcceptHeaderBuilder.java'>uk.ac.manchester.cs.AcceptHeaderBuilder</a>
     */
    public static class AcceptHeaderBuilder {
        public static String headersFromParsers(Iterable<OWLParserFactory> parsers) {
            Map<String, TreeSet<Integer>> map = new HashMap<>();
            parsers.forEach(p -> addToMap(map, p.getMIMETypes()));
            return map.entrySet().stream().sorted(AcceptHeaderBuilder::compare)
                    .map(AcceptHeaderBuilder::tostring).collect(Collectors.joining(", "));
        }

        private static void addToMap(Map<String, TreeSet<Integer>> map, List<String> mimes) {
            // The map will contain all mime types with their position in all lists mentioning them;
            // the smallest position first
            for (int i = 0; i < mimes.size(); i++) {
                map.computeIfAbsent(mimes.get(i), k -> new TreeSet<>()).add(i + 1);
            }
        }

        private static String tostring(Map.Entry<String, TreeSet<Integer>> e) {
            return String.format("%s; q=%.1f", e.getKey(), 1D / e.getValue().first());
        }

        private static int compare(Map.Entry<String, TreeSet<Integer>> a, Map.Entry<String, TreeSet<Integer>> b) {
            return a.getValue().first().compareTo(b.getValue().first());
        }
    }
}
