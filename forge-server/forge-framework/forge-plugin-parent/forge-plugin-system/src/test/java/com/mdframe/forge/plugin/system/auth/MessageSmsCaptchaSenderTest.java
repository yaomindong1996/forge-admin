package com.mdframe.forge.plugin.system.auth;

import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageSmsCaptchaSenderTest {

    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";
    private static final Duration DURATION = Duration.ofMinutes(5);

    @Test
    void shouldSendVerificationCodeThroughSmsChannel() {
        MessageClient messageClient = mock(MessageClient.class);
        when(messageClient.send(any())).thenReturn(MessageChannel.SendResult.ok("sms-1"));
        MessageSmsCaptchaSender sender = new MessageSmsCaptchaSender(messageClient);

        assertThat(sender.sendVerificationCode(PHONE, CODE, DURATION)).isTrue();

        ArgumentCaptor<MessageChannel.SendRequest> requestCaptor =
                ArgumentCaptor.forClass(MessageChannel.SendRequest.class);
        verify(messageClient).send(requestCaptor.capture());
        MessageChannel.SendRequest request = requestCaptor.getValue();
        assertThat(request.getChannel()).isEqualTo("SMS");
        assertThat(request.getType()).isEqualTo("SMS");
        assertThat(request.getPhoneList()).containsExactly(PHONE);
        assertThat(request.getContent()).contains("${code}", "${expireMinutes}");
        assertThat(request.getParams())
                .containsEntry("code", CODE)
                .containsEntry("expireMinutes", 5L);
    }

    @Test
    void shouldReturnFalseWhenMessageChannelRejectsRequest() {
        MessageClient messageClient = mock(MessageClient.class);
        when(messageClient.send(any())).thenReturn(MessageChannel.SendResult.fail("provider rejected"));
        MessageSmsCaptchaSender sender = new MessageSmsCaptchaSender(messageClient);

        assertThat(sender.sendVerificationCode(PHONE, CODE, DURATION)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenMessageClientThrows() {
        MessageClient messageClient = mock(MessageClient.class);
        when(messageClient.send(any())).thenThrow(new IllegalStateException("provider unavailable"));
        MessageSmsCaptchaSender sender = new MessageSmsCaptchaSender(messageClient);

        assertThat(sender.sendVerificationCode(PHONE, CODE, DURATION)).isFalse();
    }
}
