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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.plugins.git.GitStatus;
import hudson.plugins.git.GitStatus.ResponseContributor;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.model.Jenkins;

/**
 * Trigger builds when git push notification
 */
public class GitStatusTrigger extends Trigger<Item> {
    @Nonnull
    private static final Logger LOG = Logger.getLogger(GitStatusTrigger.class.getName());
    @CheckForNull
    private static List<GitStatusTrigger> allCache = null;

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
        clearCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        super.stop();
        clearCache();
    }

    private static void clearCache() {
        allCache = null;
    }

    private static void BroadCastNotifyAll(URIish uri, String[] branches) {
        List<GitStatusTrigger> cache = getItemsToNotify();
        if (cache == null) {
            // In case Jenkins.instance == null
            LOG.warning("Ignore push notification as Jenkins is not ready.");
            return;
        }
        for (GitStatusTrigger t: cache) {
            t.onNotifyCommit(uri, branches);
        }
    }

    @CheckForNull
    private synchronized static List<GitStatusTrigger> getItemsToNotify() {
        if (allCache == null) {
            allCache = scanItemsToNotify();
        }
        return allCache;
    }

    @CheckForNull
    private static List<GitStatusTrigger> scanItemsToNotify() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return null;
        }
        List<GitStatusTrigger> scanned = new ArrayList<GitStatusTrigger>();
        for (Item item: jenkins.getAllItems()) {
            GitStatusTrigger t = getGitStatusTrigger(item);
            if (t != null) {
                scanned.add(t);
            }
        }
        return scanned;
    }

    private static GitStatusTrigger getGitStatusTrigger(Item item) {
        if (item instanceof AbstractProject) {
            return ((AbstractProject<?, ?>)item).getTrigger(GitStatusTrigger.class);
        }
        // Support Map<TriggerDescriptor,Trigger<?>> getTriggers()
        // In case of ParameterizedJobMixIn.ParameterizedJob
        Method m = null;
        try {
            m = item.getClass().getMethod("getTriggers");
        } catch(NoSuchMethodException e) {
            return null;
        } catch(SecurityException e) {
            return null;
        }
        Object ret = null;
        try {
            ret = m.invoke(item);
        } catch (IllegalAccessException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
        if (ret == null) {
            return null;
        }
        if (!(ret instanceof Map)) {
            return null;
        }
        for (Object t: ((Map<?, ?>)ret).values()) {
            if (t instanceof GitStatusTrigger) {
                return (GitStatusTrigger)t;
            }
        }
        return null;
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
        if (job == null) {
            // Strange case that start() is not called.
            return;
        }
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
            SecurityContext orig = ACL.impersonate(ACL.SYSTEM);
            try {
                GitStatusTrigger.BroadCastNotifyAll(uri, branches);
            } finally {
                SecurityContextHolder.setContext(orig);
            }
            return Collections.emptyList();
        }
    }
}
