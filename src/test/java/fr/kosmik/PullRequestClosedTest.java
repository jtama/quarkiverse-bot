package fr.kosmik;


import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.ReactionContent;
import org.mockito.Answers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@GitHubAppTest(defaultAnswers = Answers.RETURNS_DEEP_STUBS)
public class PullRequestClosedTest {

    @Test
    void it_should_create_branch_and_pr_when_input_is_elligible_with_version_as_input() throws IOException {
        GHContent content = Mockito.mock(GHContent.class);
        BDDMockito.given(content.read()).willReturn(new ByteArrayInputStream(
                "---\nname: \"github-app-playground\"\nrelease:\n  current-version: \"1.1.0\"\n  next-version: \"1.1.1-SNAPSHOT\"\n"
                        .getBytes(StandardCharsets.UTF_8)));
        given()
                .github(mocks -> {
                    GHRepository repo = mocks.repository("362009288");
                    Mockito.when(repo.getFileContent(anyString(), isNull())).thenReturn(content);
                    GHIssue issue = mocks.issue(15);
                    Mockito.when(issue.getNumber()).thenReturn(15);
                    Mockito.when(issue.getRepository()).thenReturn(repo);
                    GHIssueComment comment = mocks.ghObject(GHIssueComment.class, 12);
                    Mockito.when(comment.getParent()).thenReturn(issue);
                    Mockito.when(comment.getBody()).thenReturn("@Quarkiverse - release - 1.0.0");
                    List<GHIssueComment> comments = Collections.singletonList(comment);
                    Mockito.when(mocks.pullRequest(630405892).getComments()).thenReturn(comments);
                    Mockito.when(mocks.pullRequest(630405892).isMerged()).thenReturn(true);
                })
                .when().payloadFromClasspath("/pr-closed.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
            GHRepository repo = mocks.repository("362009288");
            verify(repo).createRef("refs/heads/release/1.0.0", "8733376b6f04367f232d9eb46dceaa10d9ec8884");
            verify(repo).createPullRequest("Release 1.0.0",
                    "release/1.0.0",
                    "main",
                    "## Release github-app-playground 1.0.0\r\nCloses #15",
                    true);
        });
    }

    @Test
    void it_should_create_branch_and_pr_when_input_is_elligible_infering_version() throws IOException {
        GHContent content = Mockito.mock(GHContent.class);
        BDDMockito.given(content.read()).willReturn(new ByteArrayInputStream(
                "---\nname: \"github-app-playground\"\nrelease:\n  current-version: \"1.1.0\"\n  next-version: \"1.1.1-SNAPSHOT\"\n"
                        .getBytes(StandardCharsets.UTF_8)));
        given()
                .github(mocks -> {
                    GHRepository repo = mocks.repository("362009288");
                    Mockito.when(repo.getRef("heads/main")).thenReturn(Mockito.mock(GHRef.class));
                    Mockito.when(repo.getFileContent(anyString(), isNull())).thenReturn(content);
                    GHIssue issue = mocks.issue(15);
                    Mockito.when(issue.getNumber()).thenReturn(15);
                    Mockito.when(issue.getRepository()).thenReturn(repo);
                    GHIssueComment comment = mocks.ghObject(GHIssueComment.class, 12);
                    Mockito.when(comment.getParent()).thenReturn(issue);
                    Mockito.when(comment.getBody()).thenReturn("@Quarkiverse - release");
                    List<GHIssueComment> comments = Collections.singletonList(comment);
                    Mockito.when(mocks.pullRequest(630405892).getComments()).thenReturn(comments);
                    Mockito.when(mocks.pullRequest(630405892).isMerged()).thenReturn(true);
                })
                .when().payloadFromClasspath("/pr-closed.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks -> {
            GHRepository repo = mocks.repository("362009288");
            verify(repo).createRef("refs/heads/release/1.1.1", "8733376b6f04367f232d9eb46dceaa10d9ec8884");
            verify(repo).createPullRequest("Release 1.1.1",
                    "release/1.1.1",
                    "main",
                    "## Release github-app-playground 1.1.1\r\nCloses #15",
                    true);
        });
    }

    @Test
    void it_should_do_nothing_when_pr_is_not_closed() throws IOException {
        given()
                .when().payloadFromClasspath("/comment-added-to-pr.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
            verify(mocks.ghObject(GHIssueComment.class, 831750736)).createReaction(ReactionContent.ROCKET);
            verifyNoMoreInteractions(mocks.ghObjects());
        });
    }

    @Test
    void it_should_not_add_reaction_when_comment_is_not_linked_to_pr() throws IOException {
        given()
                .when().payloadFromClasspath("/comment-added.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
            verifyNoMoreInteractions(mocks.ghObjects());
        });
    }
}
