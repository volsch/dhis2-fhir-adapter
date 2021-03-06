package org.dhis2.fhir.adapter.fhir.repository;

/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Specifies the FHIR repository operation to be performed.
 *
 * @author volsch
 */
public class FhirRepositoryOperation implements Serializable
{
    private static final long serialVersionUID = 6904843568651565882L;

    private final FhirRepositoryOperationType operationType;

    private final DhisFhirResourceId dhisFhirResourceId;

    public FhirRepositoryOperation( @Nonnull FhirRepositoryOperationType operationType )
    {
        this( operationType, null );
    }

    public FhirRepositoryOperation( @Nonnull FhirRepositoryOperationType operationType, @Nullable DhisFhirResourceId dhisFhirResourceId )
    {
        this.operationType = operationType;
        this.dhisFhirResourceId = dhisFhirResourceId;
    }

    @Nonnull
    public FhirRepositoryOperationType getOperationType()
    {
        return operationType;
    }

    @Nullable
    public DhisFhirResourceId getDhisFhirResourceId()
    {
        return dhisFhirResourceId;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        FhirRepositoryOperation that = (FhirRepositoryOperation) o;

        return getOperationType() == that.getOperationType() && Objects.equals( getDhisFhirResourceId(), that.getDhisFhirResourceId() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( getOperationType(), getDhisFhirResourceId() );
    }
}
