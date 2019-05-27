package org.dhis2.fhir.adapter.fhir.metadata.service.impl;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirResourceMapping;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirResourceType;
import org.dhis2.fhir.adapter.fhir.metadata.model.MappedTrackerProgram;
import org.dhis2.fhir.adapter.fhir.metadata.model.ProgramStageRule;
import org.dhis2.fhir.adapter.fhir.metadata.repository.AdapterRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.FhirResourceMappingRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.MappedTrackerProgramRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.ProgramStageRuleRepository;
import org.dhis2.fhir.adapter.fhir.metadata.service.MetadataExportParams;
import org.dhis2.fhir.adapter.fhir.metadata.service.MetadataExportService;
import org.dhis2.fhir.adapter.fhir.metadata.service.MetadataVersionInfo;
import org.dhis2.fhir.adapter.fhir.security.AdapterSystemAuthenticationToken;
import org.dhis2.fhir.adapter.jackson.AdapterBeanPropertyFilter;
import org.dhis2.fhir.adapter.jackson.SecuredPropertyFilter;
import org.dhis2.fhir.adapter.model.Metadata;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * Default implementation of {@link MetadataExportService}.
 *
 * @author volsch
 */
@Service
public class MetadataExportServiceImpl implements MetadataExportService
{
    public static final String GIT_COMMIT_ID_PROPERTY_NAME = "git.commit.id";

    private final MappedTrackerProgramRepository trackerProgramRepository;

    private final ProgramStageRuleRepository programStageRuleRepository;

    private final FhirResourceMappingRepository fhirResourceMappingRepository;

    private final Map<Class<? extends Metadata>, String> repositoryNames;

    private final BuildProperties buildProperties;

    private final ObjectMapper mapper;

    private final PropertyFilter emptyPropertyFilter = new SimpleBeanPropertyFilter()
    {
    };

    public MetadataExportServiceImpl( @Nonnull MappedTrackerProgramRepository trackerProgramRepository,
        @Nonnull ProgramStageRuleRepository programStageRuleRepository,
        @Nonnull FhirResourceMappingRepository fhirResourceMappingRepository,
        @Nonnull List<? extends AdapterRepository<? extends Metadata<UUID>>> repositories,
        @Nullable BuildProperties buildProperties )
    {
        this.trackerProgramRepository = trackerProgramRepository;
        this.programStageRuleRepository = programStageRuleRepository;
        this.fhirResourceMappingRepository = fhirResourceMappingRepository;
        this.buildProperties = buildProperties;

        SecurityContextHolder.getContext().setAuthentication( new AdapterSystemAuthenticationToken() );
        try
        {
            final List<? extends AdapterRepository<? extends Metadata<UUID>>> filteredRepositories = repositories.stream()
                .filter( r -> AnnotationUtils.findAnnotation( r.getClass(), RepositoryRestResource.class ) != null ).collect( Collectors.toList() );
            this.repositoryNames = filteredRepositories.stream().collect(
                Collectors.toMap( AdapterRepository::getEntityType, this::getMetadataPluralNameByRepository ) );
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }

        mapper = new ObjectMapper();
        mapper.disable( FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS );
        mapper.disable( WRITE_DATES_AS_TIMESTAMPS );

        final SimpleModule module = new SimpleModule();
        module.addSerializer( InstantSerializer.INSTANCE );
        module.addSerializer( LocalDateTimeSerializer.INSTANCE );
        mapper.registerModule( module );
    }

    @Nonnull
    @Override
    @Transactional( readOnly = true, isolation = Isolation.REPEATABLE_READ )
    public JsonNode export( @Nonnull MetadataExportParams params )
    {
        final TypedMetadataObjectContainer typedContainer = new TypedMetadataObjectContainer();

        final List<MappedTrackerProgram> trackerPrograms = params.getTrackerProgramIds().isEmpty() ?
            trackerProgramRepository.findAll() : trackerProgramRepository.findAllById( params.getTrackerProgramIds() );
        typedContainer.addObjects( trackerPrograms );

        if ( !trackerPrograms.isEmpty() )
        {
            typedContainer.addObjects( programStageRuleRepository.findAllByProgram( trackerPrograms ) );
            typedContainer.addObjects( trackerPrograms.stream().map( MappedTrackerProgram::getTrackedEntityRule )
                .collect( Collectors.toList() ) );
        }

        processFhirResourceTypes( typedContainer );

        final Set<Class<? extends Metadata>> processedTypes = new HashSet<>();
        final Map<Class<? extends Metadata>, JsonNode> typedNodes = new HashMap<>();

        do
        {
            final Set<Class<? extends Metadata>> remainingTypes = new HashSet<>( typedContainer.getTypes() );
            remainingTypes.removeAll( processedTypes );

            remainingTypes.forEach( type -> {
                final ObjectWriter objectWriter = mapper.writerFor( type ).with( new SimpleFilterProvider()
                    .addFilter( SecuredPropertyFilter.FILTER_NAME, emptyPropertyFilter )
                    .addFilter( AdapterBeanPropertyFilter.FILTER_NAME,
                        new MetadataExportPropertyFilter( params, repositoryNames.keySet(), typedContainer ) ) );

                final ArrayNode nodes = JsonNodeFactory.instance.arrayNode();
                new ArrayList<>( typedContainer.getContainer( type ).getObjects() )
                    .forEach( o -> nodes.add( valueToTree( o, objectWriter ) ) );

                if ( typedNodes.put( type, nodes ) != null )
                {
                    throw new IllegalStateException( "Type " + type + " has already been processed." );
                }

                processedTypes.add( type );
            } );
        }
        while ( !processedTypes.containsAll( typedContainer.getTypes() ) );

        final ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.set( MetadataVersionInfo.VERSION_INFO_FIELD_NAME, mapper.valueToTree( createVersionInfo() ) );
        typedNodes.forEach( ( type, node ) -> rootNode.set( getMetadataPluralName( type ), node ) );

        return rootNode;
    }

    @Nonnull
    protected MetadataVersionInfo createVersionInfo()
    {
        final MetadataVersionInfo versionInfo = new MetadataVersionInfo();

        versionInfo.setExportedAt( Instant.now() );

        if ( buildProperties != null )
        {
            versionInfo.setVersion( buildProperties.getVersion() );
            versionInfo.setBuiltAt( buildProperties.getTime() );
            versionInfo.setCommitId( buildProperties.get( GIT_COMMIT_ID_PROPERTY_NAME ) );
        }

        return versionInfo;
    }

    protected void processFhirResourceTypes( @Nonnull TypedMetadataObjectContainer typedContainer )
    {
        final Map<FhirResourceType, Set<FhirResourceType>> fhirResourceMappingKeys = new HashMap<>();
        typedContainer.getContainer( ProgramStageRule.class ).getObjects().stream().map( r -> (ProgramStageRule) r ).forEach( r ->
            fhirResourceMappingKeys.computeIfAbsent( r.getProgramStage().getProgram().getTrackedEntityFhirResourceType(),
                resourceType -> new HashSet<>( Collections.singleton( resourceType ) ) ).add( r.getFhirResourceType() ) );
        fhirResourceMappingKeys.forEach( ( trackedEntityFhirResourceType, fhirResourceTypes ) ->
            typedContainer.getContainer( FhirResourceMapping.class ).addObjects(
                fhirResourceMappingRepository.findAllByFhirResourceTypes( fhirResourceTypes, trackedEntityFhirResourceType ) ) );
    }

    @Nonnull
    protected String getMetadataPluralName( @Nonnull Class<? extends Metadata> metadataClass )
    {
        final String name = repositoryNames.get( metadataClass );

        if ( name == null )
        {
            throw new IllegalStateException( "No name has been defined for metadata class " + metadataClass.getName() + "." );
        }

        return name;
    }

    @Nonnull
    protected String getMetadataPluralNameByRepository( @Nonnull AdapterRepository<? extends Metadata<UUID>> repository )
    {
        final RepositoryRestResource repositoryRestResource =
            Objects.requireNonNull( AnnotationUtils.findAnnotation( repository.getClass(), RepositoryRestResource.class ) );

        String plural = repositoryRestResource.collectionResourceRel();

        if ( StringUtils.isBlank( plural ) )
        {
            plural = StringUtils.uncapitalize( repository.getEntityType().getSimpleName() ) + "s";
        }

        return plural;
    }

    @Nonnull
    protected JsonNode valueToTree( @Nullable Object fromValue, @Nonnull ObjectWriter writer )
    {
        if ( fromValue == null )
        {
            return JsonNodeFactory.instance.nullNode();
        }

        final TokenBuffer buf = new TokenBuffer( mapper, false );
        JsonNode result;

        try
        {
            writer.writeValue( buf, fromValue );
            final JsonParser p = buf.asParser();
            result = mapper.readTree( p );
            p.close();
        }
        catch ( IOException e )
        {
            // should never occur since an internal buffer is processed
            throw new IllegalArgumentException( e.getMessage(), e );
        }

        return result;
    }
}
