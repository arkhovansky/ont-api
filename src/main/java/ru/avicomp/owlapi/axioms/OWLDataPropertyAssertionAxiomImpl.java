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
package ru.avicomp.owlapi.axioms;

import org.semanticweb.owlapi.model.*;
import ru.avicomp.owlapi.objects.ce.OWLDataHasValueImpl;
import ru.avicomp.owlapi.objects.ce.OWLObjectOneOfImpl;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public class OWLDataPropertyAssertionAxiomImpl extends OWLIndividualRelationshipAxiomImpl<OWLDataPropertyExpression, OWLLiteral> implements OWLDataPropertyAssertionAxiom {

    /**
     * @param subject     subject
     * @param property    property
     * @param value       value
     * @param annotations annotations
     */
    public OWLDataPropertyAssertionAxiomImpl(OWLIndividual subject,
                                             OWLDataPropertyExpression property,
                                             OWLLiteral value, Collection<OWLAnnotation> annotations) {
        super(subject, property, value, annotations);
    }

    @Override
    public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
        return new OWLSubClassOfAxiomImpl(new OWLObjectOneOfImpl(getSubject()),
                new OWLDataHasValueImpl(getProperty(), getObject()), NO_ANNOTATIONS);
    }

    @Override
    public OWLDataPropertyAssertionAxiom getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new OWLDataPropertyAssertionAxiomImpl(getSubject(), getProperty(), getObject(), NO_ANNOTATIONS);
    }

    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(Stream<OWLAnnotation> anns) {
        return (T) new OWLDataPropertyAssertionAxiomImpl(getSubject(), getProperty(), getObject(),
                mergeAnnos(anns));
    }
}
