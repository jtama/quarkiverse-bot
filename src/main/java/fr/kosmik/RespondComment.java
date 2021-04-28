package fr.kosmik;

import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

public class RespondComment {

    @ConfigProperty(name = "QUARKUS_GITHUB_APP_APP_NAME")
    String botName;

    public void onIssue(@Issue.Opened @Issue.Edited GHEventPayload.Issue issuePayload) throws IOException {
        addComment(issuePayload.getIssue(), "I see you created an issue");
    }

    public void onComment(@IssueComment.Created @IssueComment.Edited GHEventPayload.IssueComment issueCommentPayload) throws IOException {
        if (botName.equals(StringUtils.substringBefore(issueCommentPayload.getComment().getUser().getLogin(), "[bot]"))) {
            return;
        }
        String body = issueCommentPayload.getComment().getBody();
        if (body.toLowerCase().startsWith("@quarkiverse")) {
            processComment(issueCommentPayload);
        }
    }

    private void processComment(GHEventPayload.IssueComment issueComment) throws IOException {
        String[] commandAndArgs = issueComment.getComment().getBody().toLowerCase().split(" - ");
        switch (commandAndArgs[1]) {
            case "release":
                if (commandAndArgs.length != 3) {
                    addComment(issueComment.getIssue(), "Of course but from  which branch ?");
                    return;
                }
                release(issueComment.getRepository(), commandAndArgs[2]);
            default:
                addComment(issueComment.getIssue(),"💀");
        }
    }

    private void addComment(GHIssue issue, String comment) throws IOException {
        issue.comment(comment);
    }

    private void release(GHRepository repository, String source) throws IOException {
        String sha = repository.getRef("heads/" + source).getObject().getSha();
        String branchName = "release/1.2";
        GHRef target = repository.createRef(branchName, sha);
        repository.createContent().content(branchName).message(branchName).path(branchName).branch(branchName).commit();
        repository.createPullRequest("Release 1.2", branchName, source, getPRDesc(repository), true);
    }

    private String getPRDesc(GHRepository repo){
        return String.format("## Release %s %s", repo.getName(), "1.2");
    }
}
