package org.rtctasks.core;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.LogFactory;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.IQueryCommon;
import com.ibm.team.workitem.common.expression.*;
import com.ibm.team.workitem.common.model.*;
import com.ibm.team.workitem.common.query.*;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rtctasks.ProgressMonitor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RTCProject {

    private static final Log LOGGER = LogFactory.getLog(RTCProject.class.getName());//to use LogFactory from jazz ,this must happen after startup
    private final IWorkItemClient workItemClient;
    private final IProjectArea projectArea;
    private final IQueryableAttributeFactory _factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
    private final IProgressMonitor monitor;
    private final Set<Identifier<IState>> allUnresolvedStates;
    private final ITeamRepository teamRepository;
    private final Map<String, ITeamArea> teamAreas;

    public RTCProject(IWorkItemClient workItemClient, IProjectArea projectArea, Map<String, ITeamArea> teamAreas, IProgressMonitor monitor) throws TeamRepositoryException {
        teamRepository = workItemClient.getTeamRepository();

        this.monitor = monitor;
        this.workItemClient = workItemClient;
        this.projectArea = projectArea;
        final Identifier<IState>[] allUnresolvedStates = getAllUnresolvedStatesForProject();
        if (allUnresolvedStates != null) {
            final Stream<Identifier<IState>> allUnresolvedStates1 = Stream.of(allUnresolvedStates);
            this.allUnresolvedStates = allUnresolvedStates1.collect(Collectors.toSet());
        } else {
            this.allUnresolvedStates = Collections.emptySet();
        }
        this.teamAreas = teamAreas;
    }


    private ITeamArea getTeamAreaByName(String name) {
        final Collection<ITeamArea> values = teamAreas.values();
        for (ITeamArea value : values) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        return null;
    }

    private Identifier<IState>[] getAllUnresolvedStatesForProject() throws TeamRepositoryException {
        final Identifier<IState>[] allUnResolvedStates = workItemClient.getAllUnResolvedStates(projectArea, monitor);
        return allUnResolvedStates;
    }

    public Map<String, String> getWorkItemTypes() throws TeamRepositoryException {
        final List<IWorkItemType> workItemTypes = workItemClient.findWorkItemTypes(projectArea, monitor);
        final Map<String, String> types = new HashMap<>();
        for (IWorkItemType workItemType : workItemTypes) {
            final String workItemId = workItemType.getIdentifier();
            final String displayName = workItemType.getDisplayName();
            types.put(workItemId, displayName);
        }
        return types;
    }

    public Map<String, String> getTaskStates() throws TeamRepositoryException {
        final Map<String, String> taskStates = new HashMap<>();
        LOGGER.info("Getting task states");
        final Set<String> workItemTypes = getWorkItemTypes().keySet();
        for (String workItemId : workItemTypes) {
            LOGGER.info("Work item types" + workItemId);
            final IWorkflowInfo workFlowInfo = workItemClient.getWorkflow(workItemId, projectArea, monitor);
            final Identifier<IState>[] allStateIds = workFlowInfo.getAllStateIds();
            for (Identifier<IState> stateId : allStateIds) {
                final String stringIdentifier = stateId.getStringIdentifier();
                final String stateName = workFlowInfo.getStateName(stateId);
                taskStates.put(stringIdentifier, stateName);
            }
        }
        LOGGER.info("Got task states " + taskStates);
        return taskStates;
    }

    public IWorkItemType getWorkItemType(String workItemTypeId) {
        if (StringUtils.isNotEmpty(workItemTypeId)) {
            try {
                final IWorkItemType workItemType = workItemClient.findWorkItemType(projectArea, workItemTypeId, monitor);
                return workItemType;
            } catch (TeamRepositoryException e) {
                LOGGER.error("Failed to find workItem type ", e);
                return null;
            }
        } else {
            return null;
        }
    }

    public Map<Identifier<? extends ILiteral>, ILiteral> getLiterals(String property) throws TeamRepositoryException {
        final IEnumeration<? extends ILiteral> iEnumeration = getEnumerations(property);
        final List<? extends ILiteral> enumerationLiterals = iEnumeration.getEnumerationLiterals();
        final Map<Identifier<? extends ILiteral>, ILiteral> lits = new HashMap<>(enumerationLiterals.size());
        for (ILiteral enumerationLiteral : enumerationLiterals) {
            lits.put(enumerationLiteral.getIdentifier2(), enumerationLiteral);
        }
        return lits;
    }

    public IEnumeration<? extends ILiteral> getEnumerations(final String property) throws TeamRepositoryException {
        final IAttribute attribute = workItemClient.findAttribute(projectArea, property, monitor);
        final IEnumeration<? extends ILiteral> iEnumeration = workItemClient.resolveEnumeration(attribute, monitor);
        return iEnumeration;
    }

    public boolean isOpen(IWorkItem iWorkItem) {
        final Identifier<IState> state2 = iWorkItem.getState2();
        return this.allUnresolvedStates.contains(state2);
    }


    public List<IWorkItem> executeQuery(String value, String team, ProgressMonitor progressMonitor) throws TeamRepositoryException {
        final IQueryCommon queryService = workItemClient.getQueryClient();
        final IAuditable shared;
        if (team != null) {
            shared = getTeamAreaByName(team);
        } else {
            shared = projectArea;
        }
        final List<IQueryDescriptor> sharedQueries = queryService.findSharedQueries(projectArea, List.of(shared), QueryTypes.WORK_ITEM_QUERY, IQueryDescriptor.DEFAULT_PROFILE, progressMonitor);
        //"Cloud 2 and Engine Team"
        IQueryDescriptor iQueryDescriptor = null;

        for (final IQueryDescriptor iQueryDesc : sharedQueries) {
            LOGGER.info(iQueryDesc.getName());
            if (iQueryDesc.getName().equals(value)) {
                iQueryDescriptor = iQueryDesc;
                break;
            }
        }
        if (iQueryDescriptor != null) {
            final IQueryResult<IResolvedResult<IWorkItem>> result = queryService.getResolvedQueryResults(iQueryDescriptor, IWorkItem.DEFAULT_PROFILE);//queryService.getResolvedExpressionResults(_projectArea, term, profile);
            final ResultSize resultSize = result.getResultSize(progressMonitor);
            final int totalAvailable = resultSize.getTotalAvailable();
            final List<IWorkItem> matchingWorkItems = new ArrayList<>(totalAvailable);
            while (result.hasNext(progressMonitor)) {
                final IResolvedResult<IWorkItem> next = result.next(progressMonitor);
                final IWorkItem item = next.getItem();
                matchingWorkItems.add(item);
            }
            return matchingWorkItems;
        }
        return List.of();
    }

    public List<IWorkItem> getWorkItemsBy(String value, ProgressMonitor progressMonitor) throws TeamRepositoryException {

        //        final IQueryableAttribute idAttribute = findAttribute(_projectArea, IWorkItem.ID_PROPERTY, monitor);
        //        final AttributeExpression idExpression = new AttributeExpression(idAttribute, AttributeOperation.EQUALS,Integer.parseInt(value));
        //    try {
        final IQueryableAttribute summeryAttribute = findAttribute(projectArea, IWorkItem.SUMMARY_PROPERTY, progressMonitor);
        final AttributeExpression summeryExpression = new AttributeExpression(summeryAttribute, AttributeOperation.CONTAINS, value);

//        final IQueryableAttribute descriptionAttribute = findAttribute(_projectArea, IWorkItem.DESCRIPTION_PROPERTY, monitor);
//        final AttributeExpression descriptionExpression = new AttributeExpression(descriptionAttribute, AttributeOperation.CONTAINS, value);

        final IQueryableAttribute projectAreaAttribute = findAttribute(projectArea, IWorkItem.PROJECT_AREA_PROPERTY, progressMonitor);
        final AttributeExpression projectAreaExpression = new AttributeExpression(projectAreaAttribute, AttributeOperation.EQUALS, projectArea);

        //        final Term term = new Term(Term.Operator.AND);
        //        term.add(projectAreaExpression);
        //        term.add(summeryExpression);


        final Term term = new Term(Term.Operator.AND);
        if (value != null && !value.isEmpty()) {
            final Term termOr = new Term(Term.Operator.OR);
            //   termOr.add(idExpression);
            termOr.add(summeryExpression);
            //    termOr.add(descriptionExpression);
            term.add(termOr);
        }

        term.add(projectAreaExpression);

        final IQueryCommon queryService = workItemClient.getQueryClient();
        final ItemProfile<IWorkItem> profile = IWorkItem.FULL_PROFILE;
        final IQueryResult<IResolvedResult<IWorkItem>> result = queryService.getResolvedExpressionResults(projectArea, term, profile);

        final ResultSize resultSize = result.getResultSize(progressMonitor);
        final int totalAvailable = resultSize.getTotalAvailable();
        final List<IWorkItem> matchingWorkItems = new ArrayList<>(totalAvailable);
        while (result.hasNext(progressMonitor)) {
            final IWorkItem item = result.next(progressMonitor).getItem();
            matchingWorkItems.add(item);
        }
        return matchingWorkItems;
    }

    private IQueryableAttribute findAttribute(IProjectAreaHandle projectArea, String attributeId, IProgressMonitor monitor) throws TeamRepositoryException {
        final IQueryClient queryClient = workItemClient.getQueryClient();
        final IAuditableCommon auditableCommon = queryClient.getAuditableCommon();
        return _factory.findAttribute(projectArea, attributeId, auditableCommon, monitor);
    }

    public IProjectArea getProjectArea() {
        return projectArea;
    }
}
