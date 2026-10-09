package life.hanyang.admin.feedback.controller;

import life.hanyang.core.feedback.service.FeedbackService;
import life.hanyang.core.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FeedbackAdminControllerTest {
    @Test
    void defaultsToTwentyAndReturnsPageMetadata() throws Exception {
        var service = mock(FeedbackService.class);
        when(service.getFeedbacks(null, null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        var mvc = MockMvcBuilders.standaloneSetup(new FeedbackAdminController(service)).build();
        mvc.perform(get("/api/v1/admin/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        verify(service).getFeedbacks(null, null, null, PageRequest.of(0, 20));
    }

    @Test
    void rejectsOutOfRangePaginationBeforeQuerying() throws Exception {
        var service = mock(FeedbackService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new FeedbackAdminController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (int size : new int[]{0, -1, 101, Integer.MAX_VALUE}) {
            mvc.perform(get("/api/v1/admin/feedbacks").param("size", String.valueOf(size)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/admin/feedbacks").param("page", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
