package org.rtctasks.core;

import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IWorkItem;
import org.rtctasks.RTCTask;

import java.util.Collection;
import java.util.List;

/**
 * Created by exm1110b.
 * Date: 26/04/2018, 12:46
 */
public class TestRTCConnector {

    public static void main(String[] args) throws TeamRepositoryException {
        final String url = args[0];
        final String user = args[1];
        final String pass = args[2];
        final String projectArea = args[3];
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
        keyWord=53353;
        queryByValue(connector, keyWord);

//        queryByValue(connector,"JMS");
        connector.close();
    }

    

    private static void queryByValue(RTCConnector connector, final int keyWord) throws TeamRepositoryException {

        final IWorkItem item = connector.getJazzWorkItemById(keyWord);
        final IWorkItem workingCopy = (IWorkItem) item.getWorkingCopy();
        final IWorkItemClient workItemClient =connector.getWorkItemClient();
        
        IAttribute attrIimeSpent = workItemClient.findAttribute(item.getProjectArea(), "timeSpent", null);
        long duration=2;

        workingCopy.setValue(attrIimeSpent, duration);
        
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
