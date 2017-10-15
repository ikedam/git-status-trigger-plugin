/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 * Yahoo! Inc., Tom Huybrechts, Olivier Lamy
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.jvnet.hudson.test.TestPluginManager;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

import hudson.PluginWrapper;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;

/**
 *
 */
public class GitStatusTriggerJenkinsRule extends JenkinsRule {
    public void requestGitNotification(String url, String... branches) throws FailingHttpStatusCodeException, IOException {
        // https://wiki.jenkins.io/display/JENKINS/Git+Plugin#GitPlugin-Pushnotificationfromrepository
        // http://yourserver/git/notifyCommit?url=<URL of the Git repository>[&branches=branch1[,branch2]*][&sha1=<commit ID>]
        WebClient wc = createWebClient();
        WebRequestSettings settings = new WebRequestSettings(
            UrlUtils.toUrlUnsafe(String.format("%s/git/notifyCommit", wc.getContextPath())),
            HttpMethod.GET
        );
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("url", url));
        if (branches != null && branches.length > 0) {
            parameters.add(new NameValuePair("branches", StringUtils.join(branches, ',')));
        }
        settings.setRequestParameters(parameters);
        wc.getPage(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeStyleProject createFreeStyleProject() throws IOException
    {
        return super.createFreeStyleProject();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <P extends Item> P configRoundtrip(P before) throws Exception {
        return super.configRoundtrip(before);
    }

    /**
     * Waits until Hudson finishes building everything, including those in the queue, or fail the test
     * if the specified timeout milliseconds is
     * 
     * Ported from Jenkins 1.479.
     */
    public void waitUntilNoActivityUpTo(int timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        int streak = 0;

        while (true) {
            Thread.sleep(10);
            if (isSomethingHappening())
                streak=0;
            else
                streak++;

            if (streak>5)   // the system is quiet for a while
                return;

            if (System.currentTimeMillis()-startTime > timeout) {
                List<Queue.Executable> building = new ArrayList<Queue.Executable>();
                for (Computer c : jenkins.getComputers()) {
                    for (Executor e : c.getExecutors()) {
                        if (e.isBusy())
                            building.add(e.getCurrentExecutable());
                    }
                }
                throw new AssertionError(String.format("Hudson is still doing something after %dms: queue=%s building=%s",
                        timeout, Arrays.asList(jenkins.getQueue().getItems()), building));
            }
        }
    }

    /**
     * Remove temporary directory.
     * This is needed for Jenkins < 1.482
     * 
     * https://wiki.jenkins-ci.org/display/JENKINS/Unit+Test+on+Windows
     * 
     * @see org.jvnet.hudson.test.JenkinsRule#after()
     */
    protected void after() {
        super.after();
        if(TestEnvironment.get() != null)
        {
            try
            {
                TestEnvironment.get().dispose();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    static {
        TestPluginManagerCleanup.registerCleanup();
    }

    /**
     * Cleanup the temporary directory created by org.jvnet.hudson.test.TestPluginManager.
     * Needed for Jenkins < 1.510
     *
     * Call TestPluginManagerCleanup.registerCleanup() at least once from anywhere.
     */
    public static class TestPluginManagerCleanup {
        private static Thread deleteThread = null;

        public static synchronized void registerCleanup() {
            if(deleteThread != null) {
                return;
            }
            deleteThread = new Thread("HOTFIX: cleanup " + TestPluginManager.INSTANCE.rootDir) {
                @Override public void run() {
                    if(TestPluginManager.INSTANCE != null
                            && TestPluginManager.INSTANCE.rootDir != null
                            && TestPluginManager.INSTANCE.rootDir.exists()) {
                        // Work as PluginManager#stop
                        for(PluginWrapper p: TestPluginManager.INSTANCE.getPlugins())
                        {
                            p.stop();
                            p.releaseClassLoader();
                        }
                        TestPluginManager.INSTANCE.getPlugins().clear();
                        System.gc();
                        try {
                            Util.deleteRecursive(TestPluginManager.INSTANCE.rootDir);
                        } catch (IOException x) {
                            x.printStackTrace();
                        }
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(deleteThread);
        }
    }
}
