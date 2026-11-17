package in.feekeeper.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RequestSizeLimitFilterTest {
    @Test
    void rejectsDeclaredBodiesOverConfiguredLimit() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(new RequestLimitProperties(1024), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContent(new byte[1025]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("REQUEST_TOO_LARGE");
        verifyNoInteractions(chain);
    }
}
