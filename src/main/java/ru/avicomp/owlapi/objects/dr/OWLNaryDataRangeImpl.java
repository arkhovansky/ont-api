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
package ru.avicomp.owlapi.objects.dr;

import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLNaryDataRange;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.owlapi.OWLObjectImpl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 * @since 1.2.0
 */
public abstract class OWLNaryDataRangeImpl extends OWLObjectImpl implements OWLNaryDataRange {

    private final List<OWLDataRange> operands;

    protected OWLNaryDataRangeImpl(Collection<OWLDataRange> operands) {
        this(operands.stream());
    }

    protected OWLNaryDataRangeImpl(Stream<OWLDataRange> operands) {
        this.operands = Objects.requireNonNull(operands, "operands cannot be null")
                .filter(Objects::nonNull).distinct().sorted().collect(Iter.toUnmodifiableList());
    }

    @Override
    public Stream<OWLDataRange> operands() {
        return operands.stream();
    }

    @Override
    public List<OWLDataRange> getOperandsAsList() {
        return operands;
    }
}
