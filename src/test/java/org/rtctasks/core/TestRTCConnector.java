package org.rtctasks.core;

import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tasks.Comment;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jetbrains.annotations.NotNull;
import org.rtctasks.RTCTask;
import org.rtctasks.RTCTasksRepository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by exm1110b.
 * Date: 26/04/2018, 12:46
 */

public class TestRTCConnector {
    public static final Logger LOGGER= Logger.getInstance(RTCConnector.class);
    public static class LoggerFactory implements Logger.Factory{

        @NotNull
        @Override
        public Logger getLoggerInstance(@NotNull String s) {
            return LOGGER;
        }
    }
    public static void main(String[] args) throws TeamRepositoryException {
        final String url = args[0];
        final String user = args[1];
        final String pass = args[2];
        final String projectArea = args[3];


        System.out.println(RTCTasksRepository.matchDuration("3d 2h 4m"));
        System.out.println(RTCTasksRepository.matchDuration("2h 4m"));
        System.out.println(RTCTasksRepository.matchDuration("4m"));
        //final Duration duration = RTCTasksRepository.parseDuration(3, 2, 4);
        //    System.out.println(duration);
        Logger.setFactory(LoggerFactory.class);
        final RTCConnector connector = RTCConnector.getConnector(url, user, pass, projectArea);
        //        final IWorkItem workItemBy = connector.getWorkItemBy(31297);
        //        System.out.println(workItemBy.getHTMLSummary());
        //        final RTCTask rtcTask1 = new RTCTask(workItemBy);
        //        final Comment[] comments = rtcTask1.getComments();
        //        for (Comment comment : comments) {
        //
        //            System.out.println(comment.getText());
        //        }
        int keyWord = 53354;
        //keyWord= 31005;
        keyWord = 49352;
        keyWord = 49349;
        keyWord = 53353;
        //  keyWord=49352;
        final IWorkItem item = connector.getJazzWorkItemById(keyWord);
        connector.updateTimeSpent(item,60000,"Really");
//        updateWorkItem(connector, item, new Consumer<IWorkItem>() {
//            @Override
//            public void accept(IWorkItem iWorkItem) {
//                final String comment = "Test";
//                final ITeamRepository teamRepository = connector.getWorkItemClient().getTeamRepository();
//                final IContributor iContributor = teamRepository.loggedInContributor();
//                final IComment comment1 = iWorkItem.getComments().createComment(iContributor, XMLString.createFromPlainText(comment));
//                iWorkItem.getComments().append(comment1);
//            }
//        });
        //queryByValue(connector, keyWord);

//        queryByValue(connector,"JMS");
        connector.close();
    }


    private static void updateWorkItem(RTCConnector connector, IWorkItem item, Consumer<IWorkItem> iWorkItemConsumer) throws TeamRepositoryException {
        final IWorkItemClient workItemClient = connector.getWorkItemClient();

        ///  IAttribute attrIimeSpent = workItemClient.findAttribute(item.getProjectArea(), "timeSpent", null);
        //workItem.setValue(attrIimeSpent, duration);
        //        long duration=2000;

        final IWorkItemWorkingCopyManager workItemWorkingCopyManager = workItemClient.getWorkItemWorkingCopyManager();
        workItemWorkingCopyManager.connect(item, IWorkItem.DEFAULT_PROFILE, new NullProgressMonitor());
        final WorkItemWorkingCopy workingCopy = workItemWorkingCopyManager.getWorkingCopy(item);
        final IWorkItem workItem = workingCopy.getWorkItem();
        iWorkItemConsumer.accept(workItem);


        workItemWorkingCopyManager.save(new WorkItemWorkingCopy[]{workingCopy}, new NullProgressMonitor());
        workItemWorkingCopyManager.disconnect(item);
        workingCopy.save(new NullProgressMonitor());
    }

    private static void queryByValue(RTCConnector connector, final int keyWord) throws TeamRepositoryException {

        final IWorkItem item = connector.getJazzWorkItemById(keyWord);


        final RTCTask x = new RTCTask(item, connector);


        System.out.println(x.getType());
        System.out.println(x.getProject());
        System.out.println(x.getPresentableId());
        System.out.println(x.getPresentableName());
        System.out.println(x.getIssueUrl());
        System.out.println(x.getCustomIcon());
        System.out.println(x.isClosed());
        System.out.println(x.getNumber());
        System.out.println(x.toString());
        final Comment[] comments = x.getComments();
        for (Comment comment : comments) {
            final String author = comment.getAuthor();
            final Date date = comment.getDate();
            final String text = comment.getText();

            System.out.println(author + " " + date + " " + text);
        }
        connector.updateTimeSpent(item, 5000,"Test2");
        System.out.println("");
        // System.out.println("Description " +x.getDescription());
    }

    private static void queryByValue(RTCConnector connector, final String keyWord) throws TeamRepositoryException {

        final Collection<IWorkItem> workItems = connector.getWorkItemsBy(keyWord);
        for (IWorkItem item : workItems) {

            final List<IAttributeHandle> customAttributes = item.getCustomAttributes();
            for (IAttributeHandle customAttribute : customAttributes) {

            }

            final String workItemType = item.getWorkItemType();

            final RTCTask x = new RTCTask(item, connector);

            System.out.println("Description " + x.getDescription());
        }
    }

}

