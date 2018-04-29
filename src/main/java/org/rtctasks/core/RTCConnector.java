package org.rtctasks.core;

import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.ITeamRepositoryService;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IExtensibleItem;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.transport.TeamServiceException;
import com.ibm.team.repository.transport.client.AuthenticationException;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.IQueryCommon;
import com.ibm.team.workitem.common.expression.*;
import com.ibm.team.workitem.common.model.*;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;
import com.ibm.team.workitem.common.query.ResultSize;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.tasks.CustomTaskState;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.TaskType;
import com.intellij.tasks.impl.RequestFailedException;
import com.intellij.util.ExceptionUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.rtctasks.RTCTask;

import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 15:14
 */
public class RTCConnector {

    public final static Logger LOGGER = LogManager.getLogManager().getLogger("global");
    private static final Map<String, RTCConnector> _conPool = new ConcurrentHashMap<>();
    private final static ITeamRepositoryService teamRepositoryService;


    static {
        TeamPlatform.startup();
        teamRepositoryService = TeamPlatform.getTeamRepositoryService();

    }

    private final Set<CustomTaskState> taskStates;


    static void close() {
        TeamPlatform.shutdown();
    }

    public static synchronized RTCConnector getConnector(String url, String user, String pass, String projectArea) {

        final String key = url + user + pass + projectArea;
        final RTCConnector rtcConnector = _conPool.computeIfAbsent(key, s -> {
            try {
                return new RTCConnector(url, user, pass, projectArea);
            } catch (AuthenticationException authenticationException) {
                throw new RequestFailedException(authenticationException);
            } catch (TeamServiceException e) {
                if (ExceptionUtil.causedBy(e, ClosedByInterruptException.class)) {
                    throw new ProcessCanceledException(e);
                } else {
                    throw new RequestFailedException(e);
                }
            } catch (TeamRepositoryException e) {
                throw new RequestFailedException(e);
            }
        });

        try {
            rtcConnector.checkLogin();
        } catch (AuthenticationException authenticationException) {
            throw new RequestFailedException(authenticationException);
        } catch (TeamServiceException e) {
            if (ExceptionUtil.causedBy(e, ClosedByInterruptException.class)) {
                throw new ProcessCanceledException(e);
            } else {
                throw new RequestFailedException(e);
            }
        } catch (TeamRepositoryException e) {
            throw new RequestFailedException(e);
        }

        return rtcConnector;
    }


    private final IProjectArea _projectArea;
    private final IQueryableAttributeFactory _factory;
    //    private final IWorkItemCommon _workItemCommon;
    private final IWorkItemClient _workItemClient;
    private final Set<Identifier<IState>> allUnresolvedStates;
    private final ITeamRepository _repository;
    private final IProgressMonitor monitor = new NullProgressMonitor();
    private final IProcessItemService connect;
    private final List<IWorkItemType> workItemTypes;
    private final Map<String, TaskType> taskTypeMapper = new HashMap<>();
    private final Map<String, Map<Identifier<? extends ILiteral>, ILiteral>> literals = new ConcurrentHashMap<>();

    public static List<IProjectArea> getAllProject(final String url, final String username, final String password) throws TeamRepositoryException {
        final ITeamRepository teamRepository = teamRepositoryService.getTeamRepository(url);

        teamRepository.registerLoginHandler((ITeamRepository.ILoginHandler) repository -> new ITeamRepository.ILoginHandler.ILoginInfo() {
            public String getUserId() {
                return username;
            }

            public String getPassword() {
                return password;
            }
        });
        LOGGER.info("Connecting to repository");
        final NullProgressMonitor iProgressMonitor = new NullProgressMonitor();
        teamRepository.login(iProgressMonitor);
        final IProcessItemService clientLibrary = (IProcessItemService) teamRepository.getClientLibrary(IProcessItemService.class);
        final List<IProjectArea> allProjectAreas = clientLibrary.findAllProjectAreas(null, iProgressMonitor);
        teamRepository.logout();

        return allProjectAreas;
    }


    public RTCConnector(final String url, final String username, final String password, String projectArea) throws TeamRepositoryException {
        _repository = teamRepositoryService.getTeamRepository(url);
        _repository.registerLoginHandler((ITeamRepository.ILoginHandler) repository -> new ITeamRepository.ILoginHandler.ILoginInfo() {
            public String getUserId() {
                return username;
            }

            public String getPassword() {
                return password;
            }
        });
        LOGGER.info("Connecting to repository in project " + projectArea);
        _repository.login(monitor);
        connect = (IProcessItemService) _repository.getClientLibrary(IProcessItemService.class);
        _projectArea = getProjectArea(projectArea);

        _workItemClient = (IWorkItemClient) _repository.getClientLibrary(IWorkItemClient.class);

        
        workItemTypes = _workItemClient.findWorkItemTypes(_projectArea, monitor);

        taskTypeMapper.put("defect", TaskType.BUG);
        taskTypeMapper.put("task", TaskType.FEATURE);
        taskTypeMapper.put("", TaskType.OTHER);

        LOGGER.info("Getting task states");
        taskStates = new HashSet<>();
        for (IWorkItemType workItemType : workItemTypes) {
            final String workItemId = workItemType.getIdentifier();
          //  LOGGER.info("work item types" + identifier);
            final IWorkflowInfo workFlowInfo = _workItemClient.getWorkflow(workItemId, _projectArea, monitor);
            final Identifier<IState>[] allStateIds = workFlowInfo.getAllStateIds();
            for (Identifier<IState> stateId : allStateIds) {
                final String stringIdentifier = stateId.getStringIdentifier();
                final String stateName = workFlowInfo.getStateName(stateId);
                final CustomTaskState customTaskState=new CustomTaskState(stringIdentifier,stateName);
                taskStates.add(customTaskState);
            }
        }
        LOGGER.info("Got task states "+taskStates);

        final Identifier<IState>[] allUnresolvedStates = getAllUnresolvedStates();
        if (allUnresolvedStates != null) {
            final Stream<Identifier<IState>> allUnresolvedStates1 = Stream.of(allUnresolvedStates);
            this.allUnresolvedStates = allUnresolvedStates1.collect(Collectors.toSet());
        } else {
            this.allUnresolvedStates = Collections.emptySet();
        }

        _factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
        LOGGER.info("Connected to repository " + projectArea);
    }

    public Set<CustomTaskState> getTaskStates() {
        return taskStates;
    }

    public TaskType getTaskType(IWorkItem iWorkItem) {
        final String workItemType = iWorkItem.getWorkItemType();
        return taskTypeMapper.getOrDefault(workItemType, TaskType.OTHER);
    }

    public IWorkItemType getWorkItemType(String workItemTypeId) {
        if (workItemTypeId != null && !workItemTypeId.isEmpty()) {
            try {
                final IWorkItemType workItemType = _workItemClient.findWorkItemType(_projectArea, workItemTypeId, monitor);
                return workItemType;
            } catch (TeamRepositoryException e) {
                LOGGER.log(java.util.logging.Level.SEVERE, e, () -> "Failed to find workItme type ");
                return null;
            }
        } else {
            return null;
        }
    }

    public void checkLogin() throws TeamRepositoryException {
        if (!_repository.loggedIn()) {
            _repository.login(monitor);
        }
    }

    public TaskState getTaskState(IWorkItem iWorkItem) {
        try {
            final int stateGroup = getTaskStateGroup(iWorkItem);
            final TaskState taskState = getTaskState(stateGroup);
            return taskState;
        } catch (TeamRepositoryException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, e, () -> "Failed to find workItme state");
            return TaskState.OTHER;
        }

    }

    public TaskState getTaskState(int state) {
        switch (state) {
            case IWorkflowInfo.OPEN_STATES:
                return TaskState.OPEN;
            case IWorkflowInfo.CLOSED_STATES:
                return TaskState.RESOLVED;
            case IWorkflowInfo.IN_PROGRESS_STATES:
                return TaskState.IN_PROGRESS;
            default:
                return TaskState.OTHER;
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
        final IAttribute attribute = _workItemClient.findAttribute(_projectArea, property, monitor);
        final IEnumeration<? extends ILiteral> iEnumeration = _workItemClient.resolveEnumeration(attribute, monitor);
        return iEnumeration;
    }

    public Identifier<IState>[] getAllUnresolvedStates() throws TeamRepositoryException {
        final Identifier<IState>[] allUnResolvedStates = _workItemClient.getAllUnResolvedStates(_projectArea, monitor);
        return allUnResolvedStates;
    }

    public boolean isOpen(IWorkItem iWorkItem) {
        final Identifier<IState> state2 = iWorkItem.getState2();
        return this.allUnresolvedStates.contains(state2);
    }

    public int getTaskStateGroup(IWorkItem iWorkItem) throws TeamRepositoryException {
        final Identifier<IState> state2 = iWorkItem.getState2();
        final IWorkflowInfo workFlowInfo = getWorkFlowInfo(iWorkItem);
        return workFlowInfo.getStateGroup(state2);
    }


    public IWorkflowInfo getWorkFlowInfo(IWorkItem iWorkItem) throws TeamRepositoryException {
        final IWorkflowInfo workflowInfo = _workItemClient.findWorkflowInfo(iWorkItem, monitor);
        return workflowInfo;
    }

    public ILiteral getEnumeration(final String property, Identifier identifier) {
        final Map<Identifier<? extends ILiteral>, ILiteral> identifierILiteralMap = literals.computeIfAbsent(property, s -> {
            try {
                return getLiterals(s);
            } catch (TeamRepositoryException e) {
                if (ExceptionUtil.causedBy(e, ClosedByInterruptException.class)) {
                    return null;
                } else {
                    LOGGER.severe("Problem with getting " + property);
                    return null;
                }
            }
        });
        if (identifierILiteralMap != null) {
            return identifierILiteralMap.get(identifier);
        } else {
            return null;
        }
    }

    public IProgressMonitor getMonitor() {
        return monitor;
    }

    public List<IProjectArea> getProjectsAreas() throws TeamRepositoryException {
        final List<IProjectArea> allProjectAreas = connect.findAllProjectAreas(null, monitor);
        return allProjectAreas;
    }

    private IProjectArea getProjectArea(String name) throws TeamRepositoryException {
        final List<IProjectArea> allProjectAreas = connect.findAllProjectAreas(null, monitor);//TODO: wrap in an exception and get root cause, try again if InterruptedException
        for (IProjectArea projectArea : allProjectAreas) {
            if (projectArea.getName().equals(name)) {
                return projectArea;
            }
        }
        throw new RuntimeException("No Projects were found ");
    }


    public List<IWorkItem> getWorkItemsBy(String value) throws TeamRepositoryException {
        List<IWorkItem> matchingWorkItems;
        //        final IQueryableAttribute idAttribute = findAttribute(_projectArea, IWorkItem.ID_PROPERTY, monitor);
        //        final AttributeExpression idExpression = new AttributeExpression(idAttribute, AttributeOperation.EQUALS,Integer.parseInt(value));
        //    try {
        final IQueryableAttribute summeryAttribute = findAttribute(_projectArea, IWorkItem.SUMMARY_PROPERTY, monitor);
        final AttributeExpression summeryExpression = new AttributeExpression(summeryAttribute, AttributeOperation.CONTAINS, value);

        final IQueryableAttribute descriptionAttribute = findAttribute(_projectArea, IWorkItem.DESCRIPTION_PROPERTY, monitor);
        final AttributeExpression descriptionExpression = new AttributeExpression(descriptionAttribute, AttributeOperation.CONTAINS, value);

        final IQueryableAttribute projectAreaAttribute = findAttribute(_projectArea, IWorkItem.PROJECT_AREA_PROPERTY, monitor);
        final AttributeExpression projectAreaExpression = new AttributeExpression(projectAreaAttribute, AttributeOperation.EQUALS, _projectArea);

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

        final IQueryCommon queryService = _workItemClient.getQueryClient();
        final ItemProfile<IWorkItem> profile = IWorkItem.FULL_PROFILE;
        final IQueryResult<IResolvedResult<IWorkItem>> result = queryService.getResolvedExpressionResults(_projectArea, term, profile);

        final ResultSize resultSize = result.getResultSize(monitor);
        final int totalAvailable = resultSize.getTotalAvailable();
        matchingWorkItems = new ArrayList<IWorkItem>(totalAvailable);
        while (result.hasNext(monitor)) {
            final IWorkItem item = result.next(monitor).getItem();
            matchingWorkItems.add(item);
        }
        //  } catch (TeamServiceException e) {
        //            final Throwable rootCause = ExceptionUtils.getRootCause(e);
        //            if (rootCause instanceof ClosedByInterruptException) {
        //                System.out.println("Stopped query");
        //                matchingWorkItems= new ArrayList<IWorkItem>(0);
        //                //PermissionDeniedException
        //            } else {
        //                System.out.println("another really error");
        //                throw e;
        //            }
        //    }
        return matchingWorkItems;
    }

    public IWorkItem getJazzWorkItemById(int id) throws TeamRepositoryException {
        return _workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, monitor);
    }

    public IWorkItemClient getWorkItemClient() {
        return _workItemClient;
    }

    public RTCTask getWorkItemBy(int id) throws TeamRepositoryException {
        final IWorkItem workItemById = getJazzWorkItemById(id);
        return new RTCTask(workItemById, this);
    }


    public void processResolvedResults(IQueryResult<IResolvedResult> resolvedResults) throws TeamRepositoryException {
        // Get the required client libraries
        long processed = 0;
        while (resolvedResults.hasNext(monitor)) {
            final IResolvedResult result = resolvedResults.next(monitor);
            final IWorkItem workItem = (IWorkItem) result.getItem();
            System.out.println(workItem.getHTMLSummary());
            // do something with the work item
            processed++;
        }
        System.out.println("Processedlts: " + processed);
    }

    private IQueryableAttribute findAttribute(IProjectAreaHandle projectArea, String attributeId, IProgressMonitor monitor) throws TeamRepositoryException {
        final IQueryClient queryClient = _workItemClient.getQueryClient();
        final IAuditableCommon auditableCommon = queryClient.getAuditableCommon();
        return _factory.findAttribute(projectArea, attributeId, auditableCommon, monitor);
    }

    private ItemProfile<IWorkItem> getProfile(IQueryableAttribute attribute) {
        if (!attribute.isStateExtension()) return IWorkItem.SMALL_PROFILE.createExtension(attribute.getIdentifier());
        return IWorkItem.SMALL_PROFILE.createExtension(IWorkItem.CUSTOM_ATTRIBUTES_PROPERTY, IExtensibleItem.TIMESTAMP_EXTENSIONS_QUERY_PROPERTY);
    }

    public IProjectArea getProjectArea() {
        return _projectArea;
    }
}


