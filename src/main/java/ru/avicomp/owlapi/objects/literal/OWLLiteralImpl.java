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
package ru.avicomp.owlapi.objects.literal;

import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import ru.avicomp.owlapi.OWL2DatatypeImpl;
import ru.avicomp.owlapi.OWLDataFactoryImpl;
import ru.avicomp.owlapi.OWLObjectImpl;

import javax.annotation.Nullable;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public class OWLLiteralImpl extends OWLObjectImpl implements OWLLiteral {

    private static final OWLDatatype RDF_PLAIN_LITERAL = new ru.avicomp.owlapi.OWL2DatatypeImpl(OWL2Datatype.RDF_PLAIN_LITERAL);
    private static final OWLDatatype RDF_LANG_STRING = new ru.avicomp.owlapi.OWL2DatatypeImpl(OWL2Datatype.RDF_LANG_STRING);
    private static final OWLDatatype XSD_STRING = new OWL2DatatypeImpl(OWL2Datatype.XSD_STRING);
    private final String literal;
    private final OWLDatatype datatype;
    private final String language;

    /**
     * @param literal  actual literal form
     * @param lang     language for literal, can be null
     * @param datatype datatype for literal
     */
    public OWLLiteralImpl(String literal, @Nullable String lang, @Nullable OWLDatatype datatype) {
        this.literal = literal;
        if (lang == null || lang.isEmpty()) {
            language = "";
            if (datatype == null) {
                this.datatype = XSD_STRING;
            } else {
                this.datatype = datatype;
            }
        } else {
            if (datatype != null && !(RDF_LANG_STRING.equals(datatype) || RDF_PLAIN_LITERAL.equals(datatype))) {
                // ERROR: attempting to build a literal with a language tag and
                // type different from RDF_LANG_STRING or RDF_PLAIN_LITERAL
                throw new OWLRuntimeException("Error: cannot build a literal with type: "
                        + datatype.getIRI() + " and language: " + lang);
            }
            language = lang;
            this.datatype = RDF_LANG_STRING;
        }
    }

    @Override
    public String getLiteral() {
        return literal;
    }

    @Override
    public boolean hasLang() {
        return !language.isEmpty();
    }

    @Override
    public boolean isRDFPlainLiteral() {
        return getDatatype().isRDFPlainLiteral();
    }

    @Override
    public boolean isInteger() {
        return getDatatype().isInteger();
    }

    @Override
    public boolean isBoolean() {
        return getDatatype().isBoolean();
    }

    @Override
    public boolean isDouble() {
        return getDatatype().isDouble();
    }

    @Override
    public boolean isFloat() {
        return getDatatype().isFloat();
    }

    @Override
    public int parseInteger() {
        return Integer.parseInt(getLiteral());
    }

    @Override
    public boolean parseBoolean() {
        return OWLDataFactoryImpl.asBoolean(literal);
    }

    @Override
    public double parseDouble() {
        return Double.parseDouble(literal);
    }

    @Override
    public float parseFloat() {
        if ("inf".equalsIgnoreCase(literal)) {
            return Float.POSITIVE_INFINITY;
        }
        if ("-inf".equalsIgnoreCase(literal)) {
            return Float.NEGATIVE_INFINITY;
        }
        return Float.parseFloat(literal);
    }

    @Override
    public String getLang() {
        return language;
    }

    @Override
    public boolean hasLang(@Nullable String lang) {
        if (lang == null) {
            return language.isEmpty();
        }
        return language.equalsIgnoreCase(lang.trim());
    }

    @Override
    public OWLDatatype getDatatype() {
        return datatype;
    }

    @Override
    public int initHashCode() {
        int hash = hashIndex();
        hash = OWLObject.hashIteration(hash, getDatatype().hashCode());
        hash = OWLObject.hashIteration(hash, specificHash() * 65536);
        return OWLObject.hashIteration(hash, getLang().hashCode());
    }

    private int specificHash() {
        try {
            if (isInteger()) {
                return parseInteger();
            }
            if (isDouble()) {
                return (int) parseDouble();
            }
            if (isFloat()) {
                return (int) parseFloat();
            }
            if (isBoolean()) {
                return parseBoolean() ? 1 : 0;
            }
        } catch (NumberFormatException e) {
            // it is possible that a literal does not have a value that's valid
            // for its datatype; not very useful for a consistent ontology but
            // some W3C reasoner tests use them
        }
        return getLiteral().hashCode();
    }
}
