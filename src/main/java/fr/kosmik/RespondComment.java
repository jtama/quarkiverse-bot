package fr.kosmik;

import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
                release(issueComment.getRepository(), commandAndArgs[2], issueComment.getIssue().getNumber());
                addComment(issueComment.getIssue(), "Pull request created");
                return;
            default:
                addComment(issueComment.getIssue(), "💀");
        }
    }

    private void addComment(GHIssue issue, String comment) throws IOException {
        issue.comment(comment);
    }

    private void release(GHRepository repository, String source, int issue) throws IOException {
        String sha = repository.getRef("heads/" + source).getObject().getSha();
        String targetVersion = getTargetVersion(repository, source);
        String branchName = "release/" + targetVersion;
        GHRef ref = repository.createRef("refs/heads/" + branchName, sha);
        amendProjectDescription(repository, targetVersion, branchName, ref);
        repository.createPullRequest("Release " + targetVersion, branchName, source, getPRDesc(repository, targetVersion, issue), true);
    }

    private void amendProjectDescription(GHRepository repository, String targetVersion, String branchName, GHRef ref) throws IOException {
        String path = ".github/project.yml";
        GHContent fileContent = repository.getFileContent(path, ref.getRef());
        try (InputStream is = fileContent.read()) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            content = content.replaceAll("(?m)(^\\s.current-version:\\s)(.*)", "*1"+targetVersion);
            content = content.replaceAll("(?m)(^\\s.next-version:\\s)(.*)", "$1🚀");
            repository.createContent().content(content).message("## Preparing release " + targetVersion).path(path).branch(branchName).sha(fileContent.getSha()).commit();
        }
    }

    private String getTargetVersion(GHRepository repository, String source) throws IOException {
        GHContent fileContent = repository.getFileContent(".github/project.yml", repository.getRef("heads/" + source).getRef());
        try (InputStream is = fileContent.read()) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            return StringUtils.substringBefore(Arrays.stream(content.split(System.lineSeparator()))
                            .filter(line -> line.stripLeading().startsWith("next-version"))
                            .findFirst()
                            .map(line -> StringUtils.substringAfter(line, "next-version: "))
                            .orElseThrow(() -> new IllegalArgumentException("Hoho, what is your target ???"))
                    , "-SNAPSHOT");
        }
    }

    private String getPRDesc(GHRepository repo, String targetVersion, int issue) {
        return String.format("## Release %s %s\r\nCloses #%s", repo.getName(), targetVersion, issue);
    }
}
