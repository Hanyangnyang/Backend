package life.hanyang.user.playlist.controller;

import life.hanyang.core.auth.SupabaseUserResolver;
import life.hanyang.core.global.exception.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.service.PlaylistCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlaylistCommentControllerTest {
    @Mock PlaylistCommentService comments;
    @Mock SupabaseUserResolver users;
    MockMvc mvc;
    final UUID song = UUID.randomUUID();
    final UUID author = UUID.randomUUID();
    String path;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PlaylistCommentController(comments, users))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        path = "/api/v1/playlist/songs/" + song + "/comments";
    }

    @Test void returnsRoleNumberAndIsMineWithoutAuthorDeviceId() throws Exception {
        when(users.required("Bearer fake")).thenReturn(author);
        when(comments.create(eq(song), eq(author), any())).thenReturn(new PlaylistCommentResponse(
                UUID.randomUUID(), "댓글", PlaylistCommentResponse.AuthorRole.ANONYMOUS, 1, true, Instant.now()));
        mvc.perform(post(path).header("Authorization", "Bearer fake").contentType("application/json")
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorRole").value("ANONYMOUS"))
                .andExpect(jsonPath("$.data.anonymousNumber").value(1))
                .andExpect(jsonPath("$.data.isMine").value(true))
                .andExpect(jsonPath("$.data.deviceId").doesNotExist());
    }

    @Test void missingAuthenticationIs401() throws Exception {
        when(users.required(null)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        mvc.perform(post(path).contentType("application/json").content("{\"content\":\"댓글\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(comments);
    }

    @Test void blankOrOversizeCommentIsRejectedBeforeAuthentication() throws Exception {
        for (String content : new String[]{" ", "가".repeat(201)}) {
            mvc.perform(post(path).contentType("application/json").content("{\"content\":\"" + content + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(comments, users);
    }

    @Test void rejectsInvalidPagination() throws Exception {
        for (String size : new String[]{"0", "-1", "101"}) {
            mvc.perform(get(path).param("size", size)).andExpect(status().isBadRequest());
        }
        mvc.perform(get(path).param("page", "-1")).andExpect(status().isBadRequest());
        verifyNoInteractions(comments, users);
    }

    @Test void deleteUsesAuthenticatedUser() throws Exception {
        UUID comment = UUID.randomUUID();
        when(users.required("Bearer fake")).thenReturn(author);
        mvc.perform(delete(path + "/" + comment).header("Authorization", "Bearer fake"))
                .andExpect(status().isOk());
        verify(comments).delete(song, comment, author);
    }

}
