package fr.kosmik;


import io.quarkiverse.githubapp.testing.GitHubAppTestingResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.ReactionContent;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
@QuarkusTestResource(GitHubAppTestingResource.class)
public class CommentAddedTest {

    @Test
    void it_should_add_reaction_when_comment_is_linked_to_pr_and_starts_trigger() throws IOException {
        given()
                .when().payloadFromClasspath("/trigger-comment-added-to-pr.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks -> {
            verify(mocks.ghObject(GHIssueComment.class, 831750736)).createReaction(ReactionContent.ROCKET);
            verifyNoMoreInteractions(mocks.ghObjects());
        });
    }

    @Test
    void it_should_not_add_reaction_when_comment_is_linked_to_pr_and_doesnt_starts_trigger() throws IOException {
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
                .then().github(mocks -> verifyNoMoreInteractions(mocks.ghObjects()));
    }
}
