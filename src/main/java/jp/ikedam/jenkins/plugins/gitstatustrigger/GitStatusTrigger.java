/*
 * The MIT License
 *
 * Copyright (c) 2017 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jp.ikedam.jenkins.plugins.gitstatustrigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.plugins.git.GitStatus;
import hudson.plugins.git.GitStatus.ResponseContributor;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

/**
 *
 */
public class GitStatusTrigger extends Trigger<Item> {
    private static Logger LOG = Logger.getLogger(GitStatusTrigger.class.getName());
    private static List<GitStatusTrigger> all = new ArrayList<GitStatusTrigger>();

    @Nonnull
    private final List<GitStatusTarget> targetList;

    /**
     * Creates a new instance of {@link GitStatusTrigger}
     *
     * Called via Jenkins UI.
     *
     * @param targetList Git repositories to trigger build when notified.
     */
    @DataBoundConstructor
    public GitStatusTrigger(List<GitStatusTarget> targetList) {
        this.targetList = (targetList != null) ? targetList : Collections.<GitStatusTarget>emptyList();
    }

    @Nonnull
    public List<GitStatusTarget> getTargetList() {
        return targetList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(@Nonnull Item project, boolean newInstance) {
        super.start(project, newInstance);
        all.add(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        super.stop();
        all.remove(this);
    }

    /**
     * Called when git-plugin push notification from repositories
     *
     * @param uri URI of the repository
     * @param branches branches update occurred
     */
    public void onNotifyCommit(@Nonnull URIish uri, @Nonnull String... branches) {
        for (GitStatusTarget target: getTargetList()) {
            GitStatusTriggerCause c = target.isMatch(uri, branches);
            if (c != null) {
                scheduleBuild(c);
                return;
            }
        }
    }

    private void scheduleBuild(@Nonnull GitStatusTriggerCause c) {
        if (!(job instanceof BuildableItem)) {
            LOG.log(
                Level.WARNING,
                "Push notification from {0} (branch={1}) matches {2},"
                + " but not applicable as {2} is not buildable.",
                new Object[] {
                    c.getUri(),
                    c.getBranch(),
                    job.getFullDisplayName(),
                }
            );
            return;
        }
        BuildableItem item = (BuildableItem)job;
        item.scheduleBuild(c);
    }

    /**
     * Descriptor for {@link GitStatusTrigger}
     */
    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(Item item) {
            return (item instanceof BuildableItem);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.GitStatusTrigger_DisplayName();
        }
    }

    /**
     * Listener for git push notification.
     */
    @Extension
    public static class GitStatusListenerImpl extends GitStatus.Listener {
        /**
         * {@inheritDoc}
         */
        @Override
        public List<ResponseContributor> onNotifyCommit(URIish uri, String... branches) {
            for (GitStatusTrigger t: GitStatusTrigger.all) {
                t.onNotifyCommit(uri, branches);
            }
            return Collections.emptyList();
        }
    }
}
