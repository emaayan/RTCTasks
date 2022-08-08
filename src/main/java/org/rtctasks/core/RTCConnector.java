package org.rtctasks.core;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.ITeamArea;
import com.ibm.team.process.common.ITeamAreaHandle;
import com.ibm.team.repository.client.*;
import com.ibm.team.repository.client.internal.ItemManager;
import com.ibm.team.repository.common.*;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.expression.*;
import com.ibm.team.workitem.common.model.*;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.rtctasks.ProgressMonitor;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by exm1110B.
 * Date: 17/07/2015, 15:14
 */
public class RTCConnector implements ITeamRepository.ILoginHandler, ITeamRepository.ILoginHandler.ILoginInfo {

    private static final Log LOGGER;
    private static final ITeamRepositoryService teamRepositoryService;

    static {
        TeamPlatform.startup();
        LOGGER = LogFactory.getLog(RTCConnector.class.getName());//to use LogFactory from jazz ,this must happen after startup
        teamRepositoryService = TeamPlatform.getTeamRepositoryService();
    }

    static void close() {
        TeamPlatform.shutdown();
    }


    private String user;
    private String pass;
    private final ITeamRepository repository;
    private final IProcessItemService clientLibrary;
    private final IItemManager iItemManager;

    private final IWorkItemWorkingCopyManager _workItemWorkingCopyManager;
    private final IQueryableAttributeFactory factory = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
    //    private final IWorkItemCommon _workItemCommon;
    private final IWorkItemClient workItemClient;
    private final IProgressMonitor monitor;


    private final Map<String, Map<Identifier<? extends ILiteral>, ILiteral>> literals = new ConcurrentHashMap<>();

    public RTCConnector(String url, String user, String pass) {
        this(url, user, pass, new NullProgressMonitor());
    }

    public RTCConnector(String url, String user, String pass, IProgressMonitor iProgressMonitor) {
        this.monitor = iProgressMonitor;
        this.user = user;
        this.pass = pass;
        repository = teamRepositoryService.getTeamRepository(url);
        repository.registerLoginHandler(this);
        iItemManager = repository.itemManager();
        workItemClient = (IWorkItemClient) repository.getClientLibrary(IWorkItemClient.class);
        clientLibrary = (IProcessItemService) repository.getClientLibrary(IProcessItemService.class);
        _workItemWorkingCopyManager = workItemClient.getWorkItemWorkingCopyManager();
    }

    @Override
    public ILoginInfo challenge(ITeamRepository iTeamRepository) {
        LOGGER.info("Logging into " + iTeamRepository.getRepositoryURI());
        return this;
    }

    @Override
    public String getUserId() {
        return user;
    }

    @Override
    public String getPassword() {
        return pass;
    }

    public void setUser(String user) throws TeamRepositoryException {
        this.user = user;
        checkLogin();
    }

    public void setPass(String pass) throws TeamRepositoryException {
        this.pass = pass;
        checkLogin();
    }

    public void checkLogin() throws TeamRepositoryException {

        if (!repository.loggedIn() && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(pass)) {
            repository.login(monitor);
        }
    }

    public void login() throws TeamRepositoryException {
        checkLogin();
    }

    public void logout() {
        if (repository.loggedIn()) {
            repository.logout();
            this.projects.clear();
        }
    }

    public List<IProjectArea> getProjects() throws TeamRepositoryException {
        checkLogin();
        final List<IProjectArea> allProjectAreas = clientLibrary.findAllProjectAreas(null, monitor);
        return allProjectAreas;
    }

    public IProjectArea getProjectsBy(String name) throws TeamRepositoryException {
        final List<IProjectArea> projects = getProjects();
        for (IProjectArea project : projects) {
            if (project.getName().equals(name)) {
                return project;
            }
        }
        return null;
    }

    public IProjectArea getProjectAreaBy(String uuidValue) throws TeamRepositoryException {
        final IItem itemByUUID = getItemByUUID(uuidValue, IProjectArea.ITEM_TYPE);
        return (IProjectArea) itemByUUID;
    }

    public IItem getItemByUUID(String uuidValue, final IItemType itemType, String... properties) throws TeamRepositoryException {
        checkLogin();
        final UUID uuid = UUID.valueOf(uuidValue);
        final IItemHandle itemHandle = itemType.createItemHandle(uuid, null);
        final IItem iItem = iItemManager.fetchPartialItem(itemHandle, ItemManager.DEFAULT, Arrays.asList(properties), monitor);

        return iItem;
    }

    public IContributor getContributor(IContributorHandle iContributorHandle, ProgressMonitor progressMonitor) throws TeamRepositoryException {
        try {
            checkLogin();
            final IContributor iItem = (IContributor) iItemManager.fetchPartialItem(iContributorHandle, ItemManager.DEFAULT, Collections.emptyList(), progressMonitor);
            return iItem;
        } catch (IllegalArgumentException e) {
            error("Failed to find contributor", e);
            return null;
        }
    }

    @FunctionalInterface
    public interface WorkItemUpdater {
        public void execute(IWorkItem iWorkItem) throws TeamRepositoryException;
    }

    public void updateWorkItem(IWorkItem iWorkItem, WorkItemUpdater iWorkItemConsumer) throws TeamRepositoryException {
        try {
            _workItemWorkingCopyManager.connect(iWorkItem, IWorkItem.DEFAULT_PROFILE, monitor);
            final WorkItemWorkingCopy workingCopy = _workItemWorkingCopyManager.getWorkingCopy(iWorkItem);
            final IWorkItem workItem = workingCopy.getWorkItem();
            iWorkItemConsumer.execute(workItem);
            _workItemWorkingCopyManager.save(new WorkItemWorkingCopy[]{workingCopy}, monitor);
        } finally {
            _workItemWorkingCopyManager.disconnect(iWorkItem);
        }
    }

    public void updateTimeSpent(IWorkItem iWorkItem, long timeInMs, String comment) throws TeamRepositoryException {
        updateWorkItem(iWorkItem, iWorkItem1 -> {
            final IAttribute attrIimeSpent = workItemClient.findAttribute(iWorkItem1.getProjectArea(), "timeSpent", monitor);
            iWorkItem1.setValue(attrIimeSpent, timeInMs);
            if (comment != null && !comment.isEmpty()) {
                appendComment(comment, iWorkItem1);
            }
        });
    }

    public void addComment(IWorkItem iWorkItem, String comment) throws TeamRepositoryException {
        updateWorkItem(iWorkItem, iWorkItem1 -> appendComment(comment, iWorkItem1));
    }

    private void appendComment(String comment, IWorkItem iWorkItem1) {
        final XMLString fromPlainText = XMLString.createFromPlainText(comment);
        final IContributor loggedInContributor = repository.loggedInContributor();
        final IComment iComment = iWorkItem1.getComments().createComment(loggedInContributor, fromPlainText);
        iWorkItem1.getComments().append(iComment);
    }

    public String getContributorName(IContributorHandle iContributorHandle, ProgressMonitor progressMonitor) {
        try {
            final IContributor name = getContributor(iContributorHandle, progressMonitor);
            return name != null ? name.getName() : "";
        } catch (TeamRepositoryException e) {
            error("Failed to find contributor ", e);
            return "";
        }
    }

    public int getTaskStateGroup(IWorkItem iWorkItem) throws TeamRepositoryException {
        final Identifier<IState> state2 = iWorkItem.getState2();
        final IWorkflowInfo workFlowInfo = getWorkFlowInfo(iWorkItem);
        return workFlowInfo.getStateGroup(state2);
    }


    private IWorkflowInfo getWorkFlowInfo(IWorkItem iWorkItem) throws TeamRepositoryException {
        checkLogin();
        final IWorkflowInfo workflowInfo = workItemClient.findWorkflowInfo(iWorkItem, monitor);
        return workflowInfo;
    }

    private IProjectArea getProjectArea(String name) throws TeamRepositoryException {
        checkLogin();
        final List<IProjectArea> allProjectAreas = clientLibrary.findAllProjectAreas(null, monitor);//TODO: wrap in an exception and get root cause, try again if InterruptedException
        for (IProjectArea projectArea : allProjectAreas) {
            if (projectArea.getName().equals(name)) {
                return projectArea;
            }
        }
        throw new RuntimeException("No Projects were found for  " + name);
    }

    public RTCProject getProject(String uuid) throws TeamRepositoryException {
        return getProject(uuid, null);
    }


    private final ConcurrentHashMap<String, RTCProject> projects = new ConcurrentHashMap<>();

    public RTCProject getProject(String uuid, ProgressMonitor progressMonitor) throws TeamRepositoryException {
        checkLogin();
        final RTCProject project = projects.computeIfAbsent(uuid, s -> {
            try {
                final IProjectArea projectArea = getProjectAreaBy(uuid);
                final List<ITeamAreaHandle> teamAreas = projectArea.getTeamAreas();
                final Map<String,ITeamArea> m=new HashMap<>();
                for (ITeamAreaHandle teamArea : teamAreas) {
                    final ITeamArea itemByUUID = (ITeamArea) getItemByUUID(teamArea.getItemId().getUuidValue(), ITeamArea.ITEM_TYPE,"name");
                    m.put(itemByUUID.getItemId().getUuidValue(),itemByUUID);
                }
                final RTCProject project1 = new RTCProject(workItemClient, projectArea,m, progressMonitor);
                return project1;
            } catch (TeamRepositoryException e) {
                throw new RuntimeException(e);
            }
        });

        return project;
    }


    public IWorkItem getJazzWorkItemById(int id, ProgressMonitor progressMonitor) throws TeamRepositoryException {
        checkLogin();
        return workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, progressMonitor);
    }

    public IWorkItemClient getWorkItemClient() {
        return workItemClient;
    }


    public void processResolvedResults(IQueryResult<IResolvedResult<IWorkItem>> resolvedResults) throws TeamRepositoryException {
        // Get the required client libraries
        long processed = 0;
        while (resolvedResults.hasNext(monitor)) {
            final IResolvedResult<IWorkItem> result = resolvedResults.next(monitor);
            final IWorkItem workItem =  result.getItem();
            //System.out.println(workItem.getHTMLSummary());
            // do something with the work item
            processed++;
        }
    }


    private ItemProfile<IWorkItem> getProfile(IQueryableAttribute attribute) {
        if (!attribute.isStateExtension()) return IWorkItem.SMALL_PROFILE.createExtension(attribute.getIdentifier());
        return IWorkItem.SMALL_PROFILE.createExtension(IWorkItem.CUSTOM_ATTRIBUTES_PROPERTY, IExtensibleItem.TIMESTAMP_EXTENSIONS_QUERY_PROPERTY);
    }


    protected void error(String msg) {
        LOGGER.error(msg);
    }

    protected void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }
}


