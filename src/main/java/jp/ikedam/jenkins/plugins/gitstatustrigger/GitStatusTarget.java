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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Holds information for git notification target
 */
public class GitStatusTarget extends AbstractDescribableImpl<GitStatusTarget> {
    @Nonnull
    private final String uri;
    @Nonnull
    private final String branches;

    /**
     * @param uri URI of repository
     * @param branches Comma-separated list of branches
     */
    @DataBoundConstructor
    public GitStatusTarget(String uri, String branches) {
        this.uri = StringUtils.trim(Util.fixNull(uri));
        this.branches = StringUtils.trim(Util.fixNull(branches));
    }

    /**
     * @return URI of the repository
     */
    @Nonnull
    public String getUri() {
        return uri;
    }

    /**
     * @return Comma-separated list of branches
     */
    @Nonnull
    public String getBranches() {
        return branches;
    }

    /**
     * Test whether notification matches this target
     *
     * @param uri URI of the repository
     * @param branches Affected branches. May be empty.
     * @return the cause indicating the matched target.
     */
    @CheckForNull
    public GitStatusTriggerCause isMatch(URIish uri, String... branches) {
        if (!getUri().equals(uri.toString())) {
            return null;
        }
        if (StringUtils.isBlank(getBranches())) {
            return new GitStatusTriggerCause(uri.toString(), "");
        }
        List<String> targetBranches = Lists.transform(
            Arrays.asList(StringUtils.split(getBranches(), ',')),
            new Function<String, String>() {
                public String apply(String s) {
                    return StringUtils.trim(s);
                }
            }
        );
        for (String targetBranch: targetBranches) {
            for (String branch: branches) {
                if (isMatchBranch(targetBranch, branch)) {
                    return new GitStatusTriggerCause(uri.toString(), branch);
                }
            }
        }
        return null;
    }

    /**
     * Test whether {@code targetBranch} contains {@code branch}
     *
     * @param targetBranch configured branch
     * @param branch actual branch
     * @return {@code true} if contains
     */
    protected boolean isMatchBranch(@Nonnull String targetBranch, @Nonnull String branch) {
        if (!targetBranch.contains("*")) {
            return targetBranch.equals(branch);
        }
        String pattern = StringUtils.join(
            Lists.transform(
                Arrays.asList(StringUtils.split(targetBranch, '*')),
                new Function<String, String>() {
                    public String apply(String s) {
                        return Pattern.quote(s);
                    }
                }
            ),
            ".*"
        );
        return branch.matches(pattern);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitStatusTarget> {
        @Override
        public String getDisplayName() {
            return Messages.GitStatusTarget_DisplayName();
        }

        public FormValidation doCheckUri(@QueryParameter String uri) {
            if (StringUtils.isBlank(uri)) {
                return FormValidation.error(Messages.GitStatusTarget_uri_required());
            }
            return FormValidation.ok();
        }
    }
}
