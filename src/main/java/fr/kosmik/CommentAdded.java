package fr.kosmik;

import io.quarkiverse.githubapp.event.IssueComment;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.ReactionContent;

import java.io.IOException;
import java.util.function.Predicate;

public class CommentAdded {

    @ConfigProperty(name = "QUARKUS_GITHUB_APP_APP_NAME")
    String botName;

    private Predicate<GHEventPayload.IssueComment> isIssueAttachedToMR = issueComment -> issueComment.getIssue().getPullRequest() != null;
    private Predicate<GHEventPayload.IssueComment> isAskingRelease = issueComment -> issueComment.getComment().getBody().trim().toLowerCase().startsWith("@quarkiverse - release");

    public void onComment(@IssueComment.Created @IssueComment.Edited GHEventPayload.IssueComment issueComment) throws IOException {
        if (botName.equals(StringUtils.substringBefore(issueComment.getComment().getUser().getLogin(), "[bot]"))) {
            return;
        }
        if (isIssueAttachedToMR.and(isAskingRelease).test(issueComment)) {
            issueComment.getComment().createReaction(ReactionContent.ROCKET);
        }
    }
}
