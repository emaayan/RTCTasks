package org.rtctasks;

import com.intellij.openapi.progress.ProgressIndicator;
import org.eclipse.core.runtime.IProgressMonitor;

public class ProgressMonitor implements IProgressMonitor {

    private final ProgressIndicator progressIndicator;

    public ProgressMonitor(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    @Override
    public void beginTask(String s, int i) {
        progressIndicator.setText(s);
    }

    @Override
    public void done() {
        progressIndicator.setText("DONE"); //DON'T CALL DONe here
    }

    @Override
    public void internalWorked(double v) {

    }

    @Override
    public boolean isCanceled() {
        return progressIndicator.isCanceled();
    }

    @Override
    public void setCanceled(boolean b) {
        progressIndicator.cancel();
    }

    @Override
    public void setTaskName(String s) {

    }

    @Override
    public void subTask(String s) {
        progressIndicator.setText2(s);
    }

    @Override
    public void worked(int i) {

    }
}
