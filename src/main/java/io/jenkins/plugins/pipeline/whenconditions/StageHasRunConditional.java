/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
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

package io.jenkins.plugins.pipeline.whenconditions;

import hudson.Extension;
import hudson.model.Run;
import org.codehaus.groovy.ast.expr.Expression;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.pipeline.StageStatus;
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTWhenContent;
import org.jenkinsci.plugins.pipeline.modeldefinition.parser.ASTParserUtils;
import org.jenkinsci.plugins.pipeline.modeldefinition.when.DeclarativeStageConditional;
import org.jenkinsci.plugins.pipeline.modeldefinition.when.DeclarativeStageConditionalDescriptor;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;

import static org.jenkinsci.plugins.pipeline.modeldefinition.Utils.getStageStatusMetadata;

public class StageHasRunConditional extends DeclarativeStageConditional<StageHasRunConditional> {
    private final String stageName;

    @DataBoundConstructor
    public StageHasRunConditional(@Nonnull String stageName) {
        this.stageName = stageName;
    }

    public String getStageName() {
        return stageName;
    }

    public boolean hasOtherStageExecuted(@Nonnull RunWrapper runWrapper) {
        Run<?,?> rawRun = runWrapper.getRawBuild();
        if (rawRun instanceof WorkflowRun) {
            FlowExecution execution = ((WorkflowRun) rawRun).getExecution();
            if (execution != null) {
                List<FlowNode> stageNodes = Utils.findStageFlowNodes(stageName, execution);

                // If there are any stages we've run or are running by this name...
                if (!stageNodes.isEmpty()) {
                    // Iterate over the list of stages, check if they have a skipped status, and if so, return false.
                    for (FlowNode stageNode : stageNodes) {
                        TagsAction tagsAction = stageNode.getAction(TagsAction.class);
                        if (tagsAction != null) {
                            Map<String, String> tags = tagsAction.getTags();
                            StageStatus stageStatus = getStageStatusMetadata();
                            String stageStatusValue = tags.get(stageStatus.getTagName());

                            for (String skippedTag : getStageStatusMetadata().getSkippedStageValues()) {
                                if (skippedTag.equals(stageStatusValue)) {
                                    return false;
                                }
                            }
                        }
                    }
                    // If we have at least one stage by this name and none are skipped, return true.
                    return true;
                }
            }
        }

        // Fall back on returning false.
        return false;
    }

    @Extension
    @Symbol("stageHasRun")
    public static class DescriptorImpl extends DeclarativeStageConditionalDescriptor<StageHasRunConditional> {
        @Override
        public Expression transformToRuntimeAST(@CheckForNull ModelASTWhenContent original) {
            return ASTParserUtils.transformWhenContentToRuntimeAST(original);
        }
    }

}
