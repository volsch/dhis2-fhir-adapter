package org.dhis2.fhir.adapter.fhir.metadata.repository.validator;

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

import org.apache.commons.lang3.StringUtils;
import org.dhis2.fhir.adapter.fhir.metadata.model.DataType;
import org.dhis2.fhir.adapter.fhir.metadata.model.ExecutableScript;
import org.dhis2.fhir.adapter.fhir.metadata.model.MappedTrackerProgramStage;
import org.dhis2.fhir.adapter.fhir.metadata.model.ScriptType;
import org.dhis2.fhir.adapter.fhir.metadata.model.TransformDataType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import reactor.util.annotation.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

/**
 * Spring Data REST validator for {@link MappedTrackerProgramStage}.
 *
 * @author volsch
 */
@Component
public class BeforeCreateSaveMappedTrackerProgramStageValidator extends AbstractBeforeCreateSaveValidator<MappedTrackerProgramStage> implements MetadataValidator<MappedTrackerProgramStage>
{
    public BeforeCreateSaveMappedTrackerProgramStageValidator( @Nonnull EntityManager entityManager )
    {
        super( MappedTrackerProgramStage.class, entityManager );
    }

    @Override
    public void doValidate( @Nonnull MappedTrackerProgramStage trackerProgramStage, @Nonnull Errors errors )
    {
        if ( trackerProgramStage.getProgram() == null )
        {
            errors.rejectValue( "program", "MappedTrackerProgramStage.program.null", "Program is mandatory." );
        }
        if ( trackerProgramStage.getProgramStageReference() == null )
        {
            errors.rejectValue( "programStageReference", "MappedTrackerProgramStage.programStageReference.null", "Program stage reference is mandatory." );
        }
        if ( StringUtils.isBlank( trackerProgramStage.getName() ) )
        {
            errors.rejectValue( "name", "MappedTrackerProgramStage.name.blank", "Name must not be blank." );
        }
        if ( StringUtils.length( trackerProgramStage.getName() ) > MappedTrackerProgramStage.MAX_NAME_LENGTH )
        {
            errors.rejectValue( "name", "MappedTrackerProgramStage.name.length", new Object[]{ MappedTrackerProgramStage.MAX_NAME_LENGTH }, "Name must not be longer than {0} characters." );
        }
        if ( trackerProgramStage.getProgramStageReference() == null )
        {
            errors.rejectValue( "programStageReference", "MappedTrackerProgramStage.programReference.null", "Tracker program stage reference is mandatory." );
        }
        else if ( !trackerProgramStage.getProgramStageReference().isValid() )
        {
            errors.rejectValue( "programStageReference", "MappedTrackerProgramStage.programReference.invalid", "Tracker program stage reference is not valid." );
        }

        checkValidApplicableScript( errors, "creationApplicableScript", trackerProgramStage.getCreationApplicableScript() );
        checkValidLifecycleScript( errors, "creationScript", trackerProgramStage.getCreationScript() );
        checkValidBeforeScript( errors, "beforeScript", trackerProgramStage.getBeforeScript() );
        checkValidLifecycleScript( errors, "afterScript", trackerProgramStage.getAfterScript() );
    }

    protected static void checkValidApplicableScript( @NonNull Errors errors, @Nonnull String field, @Nullable ExecutableScript executableScript )
    {
        if ( executableScript == null )
        {
            return;
        }
        if ( executableScript.getScript().getScriptType() != ScriptType.EVALUATE )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".scriptType", "Assigned script type for applicable script must be EVALUATE." );
        }
        if ( executableScript.getScript().getReturnType() != DataType.BOOLEAN )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".returnType", "Assigned return type for applicable script must be BOOLEAN." );
        }
    }

    protected static void checkValidLifecycleScript( @NonNull Errors errors, @Nonnull String field, @Nullable ExecutableScript executableScript )
    {
        if ( executableScript == null )
        {
            return;
        }
        if ( (executableScript.getScript().getScriptType() != ScriptType.EVALUATE) && (executableScript.getScript().getScriptType() != ScriptType.TRANSFORM_TO_DHIS) )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".scriptType", "Assigned script type for applicable script must be EVALUATE or TRANSFORM_TO_DHIS." );
        }
        if ( executableScript.getScript().getReturnType() != DataType.BOOLEAN )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".returnType", "Assigned return type for lifecycle script must be BOOLEAN." );
        }
        if ( (executableScript.getScript().getScriptType() == ScriptType.TRANSFORM_TO_DHIS) && (executableScript.getScript().getOutputType() != TransformDataType.DHIS_EVENT) )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".outputType", "Assigned output type of lifecycle script must be DHIS_EVENT." );
        }
    }

    protected static void checkValidBeforeScript( @NonNull Errors errors, @Nonnull String field, @Nullable ExecutableScript executableScript )
    {
        if ( executableScript == null )
        {
            return;
        }
        if ( (executableScript.getScript().getScriptType() != ScriptType.EVALUATE) && (executableScript.getScript().getScriptType() != ScriptType.TRANSFORM_TO_DHIS) )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".scriptType", "Assigned script type for applicable script must be EVALUATE or TRANSFORM_TO_DHIS." );
        }
        if ( executableScript.getScript().getReturnType() != DataType.EVENT_DECISION_TYPE )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".returnType", "Assigned return type for lifecycle script must be EVENT_DECISION_TYPE." );
        }
        if ( (executableScript.getScript().getScriptType() == ScriptType.TRANSFORM_TO_DHIS) && (executableScript.getScript().getOutputType() != TransformDataType.DHIS_EVENT) )
        {
            errors.rejectValue( field, "MappedTrackerProgramStage." + field + ".outputType", "Assigned output type of lifecycle script must be DHIS_EVENT." );
        }
    }
}
