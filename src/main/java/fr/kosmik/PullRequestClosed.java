package fr.kosmik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.kosmik.exception.GithubAPIException;
import fr.kosmik.exception.InvalideConfigurationException;
import io.quarkiverse.githubapp.event.PullRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class PullRequestClosed {

    private static final String PROJECT_CONFIGURATION_PATH = ".github/project.yml";
    private ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    public void onMRAccepted(@PullRequest.Closed GHEventPayload.PullRequest pullRequest) throws IOException {
        if (!pullRequest.getPullRequest().isMerged())
            return;
        getAskingRelease(pullRequest.getPullRequest().getComments())
                .map(GHIssueComment::getParent)
                .ifPresent(item -> release(item, pullRequest.getPullRequest()));
    }

    private Optional<GHIssueComment> getAskingRelease(List<GHIssueComment> comments) {
        return comments.stream()
                .filter(this::isAskingRelease)
                .findFirst();
    }

    private boolean isAskingRelease(GHIssueComment issueComment) {
        return issueComment
                .getBody()
                .trim()
                .toLowerCase()
                .startsWith("@quarkiverse - release");
    }

    private void release(GHIssue issue, GHPullRequest pr) {
        GHRepository repository = issue.getRepository();
        int issueNumber = issue.getNumber();
        try {
            String sha = pr.getBase().getSha();
            String sourceBranch = pr.getBase().getRef();
            Project project = getProject(repository, sourceBranch);
            String targetVersion = getTargetVersion(issue, project).orElseThrow(InvalideConfigurationException::new);
            String branchName = "release/" + targetVersion;
            repository.createRef("refs/heads/" + branchName, sha);
            amendProjectDescription(repository, issue, branchName, project);
            repository.createPullRequest("Release " + targetVersion, branchName, sourceBranch, getPRDesc(repository, targetVersion, issueNumber), true);
        } catch (IOException e) {
            throw GithubAPIException.fromIOException(e);
        }
    }

    private Optional<String> getTargetVersion(GHIssue issue, Project project) throws IOException {
        validateNextVersion(issue, project.release.nextVersion);
        return Optional.of(getTargetVersion(project));
    }

    private void validateNextVersion(GHIssue issue, String nextVersion) throws IOException {
        if (!nextVersion.matches("\\d+\\.\\d+\\.\\d+-SNAPSHOT")) {
            issue.comment(String.format("💢 Desired next version (%s) doesn't match expected template : `\\d+\\.\\d+\\.\\d+-SNAPSHOT`.\r\nWill not proceed.", nextVersion));
            throw new IllegalArgumentException();
        }
    }

    private String getTargetVersion(Project project) {
        return StringUtils.substringBefore(project.release.nextVersion, "-SNAPSHOT");
    }

    private void amendProjectDescription(GHRepository repository, GHIssue issue, String branchName, Project project) throws IOException {
        project.release.currentVersion = getTargetVersion(project);
        project.release.nextVersion = computeNextVersion(project);
        repository.createContent().content(objectMapper.writeValueAsString(project)).message(String.format("Updated current project version to `%s`", project.release.currentVersion)).path(PROJECT_CONFIGURATION_PATH).branch(branchName).sha(project.sha).commit();
    }

    private String computeNextVersion(Project project) {
        int lastDigit = Integer.parseInt(StringUtils.substringAfterLast(project.release.currentVersion, ".")) + 1;
        return String.format("%s.%s-SNAPSHOT", StringUtils.substringBeforeLast(project.release.currentVersion, "."), lastDigit);
    }

    private Project getProject(GHRepository repository, String source) throws IOException {
        GHContent fileContent = repository.getFileContent(PROJECT_CONFIGURATION_PATH, repository.getRef("heads/" + source).getRef());
        try (InputStream is = fileContent.read()) {
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            Project project = objectMapper.readValue(content, Project.class);
            project.sha = fileContent.getSha();
            return project;
        }
    }

    private String getPRDesc(GHRepository repo, String targetVersion, int issue) {
        return String.format("## Release %s %s\r\nCloses #%s", repo.getName(), targetVersion, issue);
    }
}