package org.rtctasks.model;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.workitem.common.model.IComment;
import com.intellij.tasks.Comment;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class RTCComment extends Comment {

    private final IComment content;
    private final String author;

    public RTCComment(IComment content, String author) {
        this.content = content;
        this.author = author;
    }

    @Override
    public String getText() {
        final XMLString htmlContent = content.getHTMLContent();
        return htmlContent.getPlainText();
    }

    @Override
    public @Nullable
    String getAuthor() {
        return author;
    }

    @Nullable
    @Override
    public Date getDate() {
        return content.getCreationDate();
    }
}
