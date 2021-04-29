package fr.kosmik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.kosmik.exception.InvalideConfigurationException;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class RespondComment {

    private static final String PROJECT_CONFIGURATION_PATH = ".github/project.yml";
    private ObjectMapper objectMapper  = new ObjectMapper(new YAMLFactory());
    @ConfigProperty(name = "QUARKUS_GITHUB_APP_APP_NAME")
    String botName;

    public void onIssue(@Issue.Opened @Issue.Edited GHEventPayload.Issue issuePayload) throws IOException {
        if (botName.equals(StringUtils.substringBefore(issuePayload.getIssue().getUser().getLogin(), "[bot]"))) {
            return;
        }
        String body = issuePayload.getIssue().getTitle();
        if (body.toLowerCase().startsWith("@quarkiverse")) {
            processComment(issuePayload);
        }
    }

    public void onComment(@IssueComment.Created @IssueComment.Edited GHEventPayload.IssueComment issueCommentPayload) throws IOException {
        if (botName.equals(StringUtils.substringBefore(issueCommentPayload.getComment().getUser().getLogin(), "[bot]"))) {
            return;
        }
        String body = issueCommentPayload.getComment().getBody();
        if (body.contains("microservice")){
            issueCommentPayload.getComment().update(body.replaceAll("microservice", "💩💩"));
        }
    }

    private void processComment(GHEventPayload.Issue issuePayload) throws IOException {
        CommandAndArgs commandAndArgs = CommandAndArgs.from(issuePayload.getIssue().getTitle().toLowerCase().split(" - "));
        String command = commandAndArgs.command.orElse(StringUtils.EMPTY);
        switch (command) {
            case "release":
                if (commandAndArgs.sourceBranch.isEmpty()){
                    addComment(issuePayload.getIssue(), "Of course but from  which branch ?");
                    return;
                }
                release(issuePayload.getIssue(), commandAndArgs.sourceBranch.get(), commandAndArgs.nextVersion);
                addComment(issuePayload.getIssue(), "Pull request created");
                return;
            default:
                addComment(issuePayload.getIssue(), "💀");
        }
    }

    private void addComment(GHIssue issue, String comment) throws IOException {
        issue.comment(comment);
    }

    private void release(GHIssue issue, String sourceBranch, Optional<String> nextVersion) throws IOException {
        GHRepository repository = issue.getRepository();
        int issueNumber = issue.getNumber();
        String sha = repository.getRef("heads/" + sourceBranch).getObject().getSha();
        Project project = getProject(repository, sourceBranch);
        String targetVersion = getTargetVersion(issue, project).orElseThrow(InvalideConfigurationException::new);
        String branchName = "release/" + targetVersion;
        GHRef ref = repository.createRef("refs/heads/" + branchName, sha);
        amendProjectDescription(repository, issue, branchName, project, nextVersion);
        repository.createPullRequest("Release " + targetVersion, branchName, sourceBranch, getPRDesc(repository, targetVersion, issueNumber), true);
    }

    private Optional<String> getTargetVersion(GHIssue issue, Project project) throws IOException {
        validateNextVersion(issue, project.release.nextVersion);
        return Optional.of(getTargetVersion(project));
    }

    private void validateNextVersion(GHIssue issue, String nextVersion) throws IOException {
        if(!nextVersion.matches("\\d+\\.\\d+\\.\\d+-SNAPSHOT")) {
            addComment(issue, String.format("💢 Desired next version (%s) doesn't match expected template : `\\d+\\.\\d+\\.\\d+-SNAPSHOT`.\r\nWill not proceed.", nextVersion));
            throw new IllegalArgumentException();
        }
    }

    private String getTargetVersion(Project project) {
        return StringUtils.substringBefore(project.release.nextVersion, "-SNAPSHOT");
    }

    private void amendProjectDescription(GHRepository repository, GHIssue issue, String branchName, Project project, Optional<String> nextVersion) throws IOException {
        project.release.currentVersion = getTargetVersion(project);
        project.release.nextVersion =  getNextVersion(issue, project, nextVersion);
        repository.createContent().content(objectMapper.writeValueAsString(project)).message(String.format("Updated current project version to `%s`", project.release.currentVersion)).path(PROJECT_CONFIGURATION_PATH).branch(branchName).sha(project.sha).commit();
    }

    private String getNextVersion(GHIssue issue, Project project, Optional<String> nextVersion) throws IOException {
        validateNextVersion(issue, nextVersion.orElse("0.0.0-SNAPSHOT"));
        return nextVersion.orElseGet(() -> computeNextVersion(project));
    }

    private String computeNextVersion(Project project) {
        int lastDigit = Integer.parseInt(StringUtils.substringAfterLast(project.release.currentVersion, ".")) + 1;
        return String.format("%s.%s-SNAPSHOT", StringUtils.substringBeforeLast(project.release.currentVersion, "."), lastDigit);
    }

    private Project getProject(GHRepository repository, String source) throws IOException {
        GHContent fileContent = repository.getFileContent(PROJECT_CONFIGURATION_PATH, repository.getRef("heads/" + source).getRef());
        try (InputStream is = fileContent.read()) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            Project project =objectMapper.readValue(content, Project.class);
            project.sha = fileContent.getSha();
            return project;
        }
    }

    private String getPRDesc(GHRepository repo, String targetVersion, int issue) {
        return String.format("## Release %s %s\r\nCloses #%s", repo.getName(), targetVersion, issue);
    }

}
