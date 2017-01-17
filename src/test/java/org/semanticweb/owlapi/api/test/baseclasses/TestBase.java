/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.api.test.baseclasses;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.semanticweb.owlapi.api.test.anonymous.AnonymousIndividualsNormaliser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFJsonLDDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asSet;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health
 *         Informatics Group
 * @since 2.2.0
 */
@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings({"javadoc", "null"})
public abstract class TestBase {

    public static final boolean DEBUG_USE_OWL = Boolean.parseBoolean(System.getProperty("debug.use.owl", Boolean.FALSE.toString()));

    protected static final Logger LOGGER = LoggerFactory.getLogger(TestBase.class);
    protected static final String uriBase = "http://www.semanticweb.org/owlapi/test";
    protected static OWLDataFactory df;
    protected static OWLOntologyManager masterManager;
    public static final File RESOURCES = resources();
    protected final OWLOntologyBuilder builder = DEBUG_USE_OWL ? (OWLOntologyBuilder) OWLOntologyImpl::new :
            (m, id) -> new ru.avicomp.ontapi.OntologyModelImpl((ru.avicomp.ontapi.OntologyManager) m, id);
    @Nonnull
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    public Timeout timeout = new Timeout(1_000_000, TimeUnit.MILLISECONDS);
    @Nonnull
    protected OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
    protected OWLOntologyManager m;
    protected OWLOntologyManager m1;

    private static File resources() {
        try {
            return new File(TestBase.class.getResource("/owlapi/owlapi.properties").toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new OWLRuntimeException("NO RESOURCE FOLDER ACCESSIBLE", e);
        }
    }

    public static OWLOntologyManager createOWLManager() {
        return DEBUG_USE_OWL ? OWLManager.createOWLOntologyManager() : ru.avicomp.ontapi.OntManagerFactory.createONTManager();
    }

    @BeforeClass
    public static void setupManagers() {
        masterManager = createOWLManager();
        df = masterManager.getOWLDataFactory();
    }

    protected static OWLOntologyManager setupManager() {
        OWLOntologyManager manager = DEBUG_USE_OWL ?
                new OWLOntologyManagerImpl(df, new NoOpReadWriteLock()) :
                new ru.avicomp.ontapi.OntologyManagerImpl(df, new NoOpReadWriteLock());
        manager.getOntologyFactories().set(masterManager.getOntologyFactories());
        manager.getOntologyParsers().set(masterManager.getOntologyParsers());
        manager.getOntologyStorers().set(masterManager.getOntologyStorers());
        manager.getIRIMappers().set(masterManager.getIRIMappers());
        manager.setOntologyConfigurator(masterManager.getOntologyConfigurator());
        return manager;
    }

    protected static <S> Set<S> singleton(S s) {
        return Collections.singleton(s);
    }

    private static Set<OWLAnnotation> reannotate(Stream<OWLAnnotation> anns) {
        OWLDatatype stringType = df.getOWLDatatype(OWL2Datatype.XSD_STRING);
        Set<OWLAnnotation> toReturn = new HashSet<>();
        anns.forEach(a -> {
            Optional<OWLLiteral> asLiteral = a.getValue().asLiteral();
            if (asLiteral.isPresent() && asLiteral.get().isRDFPlainLiteral()) {
                OWLAnnotation replacement = df.getOWLAnnotation(a.getProperty(), df.getOWLLiteral(asLiteral.get()
                        .getLiteral(), stringType));
                toReturn.add(replacement);
            } else {
                toReturn.add(a);
            }
        });
        return toReturn;
    }

    private static String topOfStackTrace() {
        StackTraceElement[] elements = new RuntimeException().getStackTrace();
        return elements[1] + "\n" + elements[2] + '\n' + elements[3];
    }

    /**
     * @param leftOnly
     * @param rightOnly
     * @return
     */
    public static boolean verifyErrorIsDueToBlankNodesId(Set<OWLAxiom> leftOnly, Set<OWLAxiom> rightOnly) {
        Set<String> leftOnlyStrings = new HashSet<>();
        Set<String> rightOnlyStrings = new HashSet<>();
        for (OWLAxiom ax : leftOnly) {
            leftOnlyStrings.add(ax.toString().replaceAll("_:anon-ind-[0-9]+", "blank").replaceAll("_:genid[0-9]+",
                    "blank"));
        }
        for (OWLAxiom ax : rightOnly) {
            rightOnlyStrings.add(ax.toString().replaceAll("_:anon-ind-[0-9]+", "blank").replaceAll("_:genid[0-9]+",
                    "blank"));
        }
        return rightOnlyStrings.equals(leftOnlyStrings);
    }

    public static IRI iri(String name) {
        return IRI(uriBase + '#', name);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalGetWithoutIsPresent"})
    protected <T> T get(Optional<T> t) {
        return t.get();
    }

    protected OWLOntology ontologyFromClasspathFile(String fileName) {
        return ontologyFromClasspathFile(fileName, config);
    }

    protected OWLOntology ontologyFromClasspathFile(String fileName, OWLOntologyLoaderConfiguration configuration) {
        try {
            return m1.loadOntologyFromOntologyDocument(new StreamDocumentSource(getClass().getResourceAsStream("/owlapi/" + fileName)), configuration);
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    @Before
    public void setupManagersClean() {
        m = setupManager();
        m1 = setupManager();
    }

    public boolean equal(OWLOntology ont1, OWLOntology ont2) {
        if (!ont1.isAnonymous() && !ont2.isAnonymous()) {
            assertEquals("Ontologies supposed to be the same", ont1.getOntologyID(), ont2.getOntologyID());
        }
        assertEquals(asSet(ont1.annotations()), asSet(ont2.annotations()));
        Set<OWLAxiom> axioms1;
        Set<OWLAxiom> axioms2;
        // This isn't great - we normalise axioms by changing the ids of
        // individuals. This relies on the fact that
        // we iterate over objects in the same order for the same set of axioms!
        axioms1 = new AnonymousIndividualsNormaliser(ont1.getOWLOntologyManager()).getNormalisedAxioms(ont1.axioms());
        axioms2 = new AnonymousIndividualsNormaliser(ont1.getOWLOntologyManager()).getNormalisedAxioms(ont2.axioms());
        OWLDocumentFormat ontologyFormat = ont2.getFormat();
        applyEquivalentsRoundtrip(axioms1, axioms2, ontologyFormat);
        if (ontologyFormat instanceof ManchesterSyntaxDocumentFormat) {
            // drop GCIs from the expected axioms, they won't be there
            Iterator<OWLAxiom> it = axioms1.iterator();
            while (it.hasNext()) {
                OWLAxiom next = it.next();
                if (next instanceof OWLSubClassOfAxiom) {
                    if (((OWLSubClassOfAxiom) next).getSubClass().isAnonymous() && ((OWLSubClassOfAxiom) next)
                            .getSuperClass().isAnonymous()) {
                        it.remove();
                    }
                }
            }
        }
        PlainLiteralTypeFoldingAxiomSet a = new PlainLiteralTypeFoldingAxiomSet(axioms1);
        PlainLiteralTypeFoldingAxiomSet b = new PlainLiteralTypeFoldingAxiomSet(axioms2);
        if (!a.equals(b)) {
            int counter = 0;
            StringBuilder sb = new StringBuilder();
            Set<OWLAxiom> leftOnly = new HashSet<>();
            Set<OWLAxiom> rightOnly = new HashSet<>();
            for (OWLAxiom ax : a) {
                if (!b.contains(ax)) {
                    if (!isIgnorableAxiom(ax, false)) {
                        leftOnly.add(ax);
                        sb.append("Rem axiom: ").append(ax).append('\n');
                        counter++;
                    }
                }
            }
            for (OWLAxiom ax : b) {
                if (!a.contains(ax)) {
                    if (!isIgnorableAxiom(ax, true)) {
                        rightOnly.add(ax);
                        sb.append("Add axiom: ").append(ax).append('\n');
                        counter++;
                    }
                }
            }
            if (counter > 0 && !rightOnly.equals(leftOnly)) {
                // a test fails on OpenJDK implementations because of
                // ordering
                // testing here if blank node ids are the only difference
                boolean fixed = !verifyErrorIsDueToBlankNodesId(leftOnly, rightOnly);
                if (fixed) {
                    if (LOGGER.isDebugEnabled()) {
                        String x = getClass().getSimpleName() + " roundTripOntology() Failing to match axioms: \n" + sb
                                + topOfStackTrace();
                        LOGGER.debug(x);
                    }
                    fail(getClass().getSimpleName() + " roundTripOntology() Failing to match axioms: \n" + sb);
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
        // assertEquals(axioms1, axioms2);
        return true;
    }

    /**
     * equivalent entity axioms with more than two entities are broken up by RDF
     * syntaxes. Ensure they are still recognized as correct roundtripping
     */
    public void applyEquivalentsRoundtrip(Set<OWLAxiom> axioms1, Set<OWLAxiom> axioms2, OWLDocumentFormat destination) {
        if (!axioms1.equals(axioms2)) {
            // remove axioms that differ only because of n-ary equivalence
            // axioms
            // http://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples
            for (OWLAxiom ax : new ArrayList<>(axioms1)) {
                if (ax instanceof OWLEquivalentClassesAxiom) {
                    OWLEquivalentClassesAxiom ax2 = (OWLEquivalentClassesAxiom) ax;
                    if (ax2.classExpressions().count() > 2) {
                        Collection<OWLEquivalentClassesAxiom> pairs = ax2.splitToAnnotatedPairs();
                        if (removeIfContainsAll(axioms2, pairs, destination)) {
                            axioms1.remove(ax);
                            axioms2.removeAll(pairs);
                        }
                    }
                } else if (ax instanceof OWLEquivalentDataPropertiesAxiom) {
                    OWLEquivalentDataPropertiesAxiom ax2 = (OWLEquivalentDataPropertiesAxiom) ax;
                    if (ax2.properties().count() > 2) {
                        Collection<OWLEquivalentDataPropertiesAxiom> pairs = ax2.splitToAnnotatedPairs();
                        if (removeIfContainsAll(axioms2, pairs, destination)) {
                            axioms1.remove(ax);
                            axioms2.removeAll(pairs);
                        }
                    }
                } else if (ax instanceof OWLEquivalentObjectPropertiesAxiom) {
                    OWLEquivalentObjectPropertiesAxiom ax2 = (OWLEquivalentObjectPropertiesAxiom) ax;
                    if (ax2.properties().count() > 2) {
                        Collection<OWLEquivalentObjectPropertiesAxiom> pairs = ax2.splitToAnnotatedPairs();
                        if (removeIfContainsAll(axioms2, pairs, destination)) {
                            axioms1.remove(ax);
                            axioms2.removeAll(pairs);
                        }
                    }
                }
            }
        }
        if (!axioms1.equals(axioms2) && destination instanceof RDFJsonLDDocumentFormat) {
            // other axioms can have their annotations changed to string type
            Set<OWLAxiom> reannotated1 = new HashSet<>();
            axioms1.forEach(a -> reannotated1.add(reannotate(a)));
            axioms1.clear();
            axioms1.addAll(reannotated1);
            Set<OWLAxiom> reannotated2 = new HashSet<>();
            axioms2.forEach(a -> reannotated2.add(reannotate(a)));
            axioms2.clear();
            axioms2.addAll(reannotated2);
        }
    }

    private boolean removeIfContainsAll(Collection<OWLAxiom> axioms, Collection<? extends OWLAxiom> others,
                                        OWLDocumentFormat destination) {
        if (axioms.containsAll(others)) {
            axioms.removeAll(others);
            return true;
        }
        // some syntaxes attach xsd:string to annotation values that did not
        // have it previously
        if (!(destination instanceof RDFJsonLDDocumentFormat)) {
            return false;
        }
        Set<OWLAxiom> toRemove = new HashSet<>();
        for (OWLAxiom ax : others) {
            OWLAxiom reannotated = reannotate(ax);
            toRemove.add(reannotated);
        }
        axioms.removeAll(toRemove);
        return true;
    }

    protected OWLAxiom reannotate(OWLAxiom ax) {
        return ax.getAxiomWithoutAnnotations().getAnnotatedAxiom(reannotate(ax.annotations()));
    }

    /**
     * ignore declarations of builtins and of named individuals - named
     * individuals do not /need/ a declaration, but addiong one is not an error.
     *
     * @param parse true if the axiom belongs to the parsed ones, false for the input
     * @return true if the axiom can be ignored
     */
    public boolean isIgnorableAxiom(OWLAxiom ax, boolean parse) {
        if (ax instanceof OWLDeclarationAxiom) {
            OWLDeclarationAxiom d = (OWLDeclarationAxiom) ax;
            if (parse) {
                // all extra declarations in the parsed ontology are fine
                return true;
            }
            // declarations of builtin and named individuals can be ignored
            return d.getEntity().isBuiltIn() || d.getEntity().isOWLNamedIndividual();
        }
        return false;
    }

    public OWLOntology getOWLOntology() {
        try {
            return m.createOntology(IRI.getNextDocumentIRI(uriBase));
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    public OWLOntology getOWLOntology(IRI iri) throws OWLOntologyCreationException {
        return m.createOntology(iri);
    }

    public OWLOntology getOWLOntology(OWLOntologyID iri) throws OWLOntologyCreationException {
        return m.createOntology(iri);
    }

    public OWLOntology getAnonymousOWLOntology() {
        try {
            return m.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    public void roundTripOntology(OWLOntology ont) throws OWLOntologyStorageException, OWLOntologyCreationException {
        roundTripOntology(ont, new RDFXMLDocumentFormat());
    }

    /**
     * see {@link #roundTrip(OWLOntology, OWLDocumentFormat)}.
     *
     * @param ont                    {@link OWLOntology} original ontology to test.
     * @param format                 {@link OWLDocumentFormat} to reload
     * @param recalculateAxiomsCache only for ONT-API.
     *                               if true clears the axioms cache inside specified {@link ru.avicomp.ontapi.OntologyModelImpl} ontology.
     * @return {@link OWLOntology} the reloaded ontology.
     * @throws OWLOntologyStorageException
     * @throws OWLOntologyCreationException
     */
    @SuppressWarnings("WeakerAccess")
    protected OWLOntology roundTripOntology(OWLOntology ont, OWLDocumentFormat format, boolean recalculateAxiomsCache)
            throws OWLOntologyStorageException, OWLOntologyCreationException {
        if (!DEBUG_USE_OWL && recalculateAxiomsCache) {
            ((ru.avicomp.ontapi.OntologyModel) ont).clearCache();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Origin(source) ontology:");
            ru.avicomp.ontapi.utils.ReadWriteUtils.print(ont);
            ont.axioms().forEach(a -> LOGGER.debug(a.toString()));
        }
        StringDocumentTarget target = new StringDocumentTarget();
        OWLDocumentFormat fromFormat = ont.getFormat();
        if (fromFormat.isPrefixOWLDocumentFormat() && format.isPrefixOWLDocumentFormat()) {
            PrefixDocumentFormat fromPrefixFormat = fromFormat.asPrefixOWLDocumentFormat();
            PrefixDocumentFormat toPrefixFormat = format.asPrefixOWLDocumentFormat();
            toPrefixFormat.copyPrefixesFrom(fromPrefixFormat);
            toPrefixFormat.setDefaultPrefix(null);
        }
        format.setAddMissingTypes(true);
        ont.saveOntology(format, target);
        String txt = target.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Target ontology:");
            LOGGER.debug(txt);
        }
        OWLOntology ont2 = setupManager().loadOntologyFromOntologyDocument(
                new StringDocumentSource(txt, "string:ontology", format, null),
                new OWLOntologyLoaderConfiguration().setReportStackTraces(true));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("TestBase.roundTripOntology() ontology parsed");
            ont2.axioms().forEach(ax -> LOGGER.debug(ax.toString()));
        }
        //ru.avicomp.ontapi.utils.TestUtils.compareAxioms(ont.axioms(), ont2.axioms());

        equal(ont, ont2);
        return ont2;
    }

    /**
     * Saves the specified ontology in the specified format and reloads it.
     * Calling this method from a test will cause the test to fail if the
     * ontology could not be stored, could not be reloaded, or was reloaded and
     * the reloaded version is not equal (in terms of ontology URI and axioms)
     * with the original.
     *
     * @param ont    The ontology to be round tripped.
     * @param format The format to use when doing the round trip.
     */
    public OWLOntology roundTripOntology(OWLOntology ont, OWLDocumentFormat format)
            throws OWLOntologyStorageException, OWLOntologyCreationException {
        return roundTripOntology(ont, format, false);
    }

    protected OWLOntology loadOntologyFromString(String input) throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input));
    }

    protected OWLOntology loadOntologyFromString(String input, IRI i, OWLDocumentFormat f) {
        StringDocumentSource documentSource = new StringDocumentSource(input, i, f, null);
        try {
            return setupManager().loadOntologyFromOntologyDocument(documentSource);
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    protected OWLOntology loadOntologyFromString(StringDocumentSource input) throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(input);
    }

    protected OWLOntology loadOntologyFromString(StringDocumentTarget input) throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input));
    }

    protected OWLOntology loadOntologyFromString(StringDocumentTarget input, OWLDocumentFormat f)
            throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(new StringDocumentSource(input.toString(),
                "string:ontology", f, null));
    }

    protected OWLOntology loadOntologyStrict(StringDocumentTarget o) throws OWLOntologyCreationException {
        return loadOntologyWithConfig(o, new OWLOntologyLoaderConfiguration().setStrict(true));
    }

    protected OWLOntology loadOntologyWithConfig(StringDocumentTarget o, OWLOntologyLoaderConfiguration c)
            throws OWLOntologyCreationException {
        return loadOntologyWithConfig(new StringDocumentSource(o), c);
    }

    protected OWLOntology loadOntologyWithConfig(StringDocumentSource o, OWLOntologyLoaderConfiguration c)
            throws OWLOntologyCreationException {
        return setupManager().loadOntologyFromOntologyDocument(o, c);
    }

    protected StringDocumentTarget saveOntology(OWLOntology o) throws OWLOntologyStorageException {
        return saveOntology(o, o.getFormat());
    }

    protected StringDocumentTarget saveOntology(OWLOntology o, OWLDocumentFormat format)
            throws OWLOntologyStorageException {
        StringDocumentTarget t = new StringDocumentTarget();
        o.getOWLOntologyManager().saveOntology(o, format, t);
        return t;
    }

    protected OWLOntology roundTrip(OWLOntology o, OWLDocumentFormat format) throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        return loadOntologyFromString(saveOntology(o, format), format);
    }

    protected OWLOntology roundTrip(OWLOntology o, OWLDocumentFormat format, OWLOntologyLoaderConfiguration c)
            throws OWLOntologyCreationException, OWLOntologyStorageException {
        return loadOntologyWithConfig(saveOntology(o, format), c);
    }

    protected OWLOntology roundTrip(OWLOntology o) throws OWLOntologyCreationException, OWLOntologyStorageException {
        return loadOntologyFromString(saveOntology(o));
    }

    public interface AxiomBuilder {

        Set<OWLAxiom> build();
    }
}
