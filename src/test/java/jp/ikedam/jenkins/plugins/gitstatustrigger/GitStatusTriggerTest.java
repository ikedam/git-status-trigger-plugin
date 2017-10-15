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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;

/**
 * Tests for {@link GitStatusTrigger}
 */
public class GitStatusTriggerTest {
    private static final int ACTIVITY_WAIT = 60000;

    @ClassRule
    public static GitStatusTriggerJenkinsRule j = new GitStatusTriggerJenkinsRule();

    @After
    public void cleanAllJobs() throws Exception {
        for (Job<?, ?> job: j.jenkins.getAllItems(Job.class)) {
            job.delete();
        }
    }

    public void testNoConfiguration() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        j.configRoundtrip((Item)p);

        assertNull(p.getTrigger(GitStatusTrigger.class));
    }

    public void testConfiguration() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        GitStatusTrigger t = new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            ),
            new GitStatusTarget(
                "git@github.com:ikedam/git-status-trigger-plugin.git",
                "branch1,branch2"
            )
        ));

        p.addTrigger(t);

        j.configRoundtrip((Item)p);

        j.assertEqualDataBoundBeans(t, p.getTrigger(GitStatusTrigger.class));
    }

    @Test
    public void testTrigger() throws Exception {
        FreeStyleProject p1 = j.createFreeStyleProject();
        FreeStyleProject p2 = j.createFreeStyleProject();

        p1.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            )
        )));

        j.configRoundtrip((Item)p1);
        j.configRoundtrip((Item)p2);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p1.getLastBuild());
        assertNull(p2.getLastBuild());
    }

    @Test
    public void testTriggerWithBranchMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "master,develop"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "develop"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithBranchMatch2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "master,develop"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "develop",
            "feature/something"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithBranchNotMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "master,develop"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "feature/something"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithBranchNotMatch2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "master,develop"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithBlankBranchMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithBlankBranchMatch2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "develop"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithWildcardBranchMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "*"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "develop"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithWildcardBranchMatch2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "feature/*/test"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "feature/newfeature/test"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerWithWildcardBranchNotMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "*"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNull(p.getLastBuild());
    }
    @Test
    public void testTriggerWithWildcardBranchNotMatch2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "feature/*/test"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "feature/newfeature"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNull(p.getLastBuild());
    }

    @Test
    public void testTriggerUrlMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerUrlNotMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/redirect404-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNull(p.getLastBuild());
    }

    @Test
    public void testTriggerMultipleTargetMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            ),
            new GitStatusTarget(
                "git@github.com:ikedam/git-status-trigger-plugin.git",
                "develop"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerMultipleTargetMatch2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            ),
            new GitStatusTarget(
                "git@github.com:ikedam/git-status-trigger-plugin.git",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "git@github.com:ikedam/git-status-trigger-plugin.git",
            "develop"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNotNull(p.getLastBuild());
    }

    @Test
    public void testTriggerMultipleTargetNotMatch1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                "master"
            ),
            new GitStatusTarget(
                "git@github.com:ikedam/git-status-trigger-plugin.git",
                "develop"
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "master"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        assertNull(p.getLastBuild());
    }

    @Test
    public void testCauseWithBranch() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            ),
            new GitStatusTarget(
                "git@github.com:ikedam/git-status-trigger-plugin.git",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin",
            "master"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        FreeStyleBuild b = p.getLastBuild();
        GitStatusTriggerCause c = b.getCause(GitStatusTriggerCause.class);
        assertNotNull(c);
        assertEquals("https://github.com/ikedam/git-status-trigger-plugin", c.getUri());
        assertEquals("master", c.getBranch());

        WebClient wc = j.createWebClient();
        wc.getPage(b);
    }

    @Test
    public void testCauseWithoutBranch() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        p.addTrigger(new GitStatusTrigger(Arrays.asList(
            new GitStatusTarget(
                "https://github.com/ikedam/git-status-trigger-plugin",
                ""
            ),
            new GitStatusTarget(
                "git@github.com:ikedam/git-status-trigger-plugin.git",
                ""
            )
        )));

        j.configRoundtrip((Item)p);

        j.requestGitNotification(
            "https://github.com/ikedam/git-status-trigger-plugin"
        );
        j.waitUntilNoActivityUpTo(ACTIVITY_WAIT);
        FreeStyleBuild b = p.getLastBuild();
        GitStatusTriggerCause c = b.getCause(GitStatusTriggerCause.class);
        assertNotNull(c);
        assertEquals("https://github.com/ikedam/git-status-trigger-plugin", c.getUri());
        assertEquals("", c.getBranch());
    }
}
