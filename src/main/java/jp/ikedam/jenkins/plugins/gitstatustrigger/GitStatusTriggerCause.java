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

import org.apache.commons.lang.StringUtils;

import hudson.model.Cause;

/**
 * Cause triggered when git push notification
 */
public class GitStatusTriggerCause extends Cause {
    private final String uri;
    private final String branch;

    /**
     * @param uri URI of notified repository
     * @param branch notified branch
     */
    public GitStatusTriggerCause(String uri, String branch) {
        this.uri = uri;
        this.branch = branch;
    }

    /**
     * @return URI of notified repository
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return notified branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @return notified branch. "(none)" for brank.
     */
    public String getBranchForDisplay() {
        String branch = getBranch();
        if (StringUtils.isBlank(branch)) {
            return "(none)";
        }
        return branch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortDescription() {
        return Messages.GitStatusTriggerCause_Description(
            getUri(),
            getBranchForDisplay()
        );
    }
}
