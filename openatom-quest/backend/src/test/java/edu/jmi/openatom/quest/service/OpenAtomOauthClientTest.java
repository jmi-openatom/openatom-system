package edu.jmi.openatom.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.quest.config.OauthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class OpenAtomOauthClientTest {
    @Test
    void missingAvatarUsesPictureAndNullIsNotStoredAsText() throws Exception {
        OpenAtomOauthClient client = new OpenAtomOauthClient(mock(OauthProperties.class), mock(RestClient.class));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode withPicture = mapper.readTree("{\"avatar\":null,\"picture\":\"https://lms.example.test/avatar.png\"}");
        JsonNode withoutAvatar = mapper.readTree("{\"avatar\":null,\"picture\":null}");

        String resolved = ReflectionTestUtils.invokeMethod(client, "firstText", withPicture,
            new String[]{"avatar", "picture"});
        String missing = ReflectionTestUtils.invokeMethod(client, "firstText", withoutAvatar,
            new String[]{"avatar", "picture"});
        assertThat(resolved).isEqualTo("https://lms.example.test/avatar.png");
        assertThat(missing).isNull();
    }
}
