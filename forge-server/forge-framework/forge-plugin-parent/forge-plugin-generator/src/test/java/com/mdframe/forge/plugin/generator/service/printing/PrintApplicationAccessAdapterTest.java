package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationService;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintApplicationAccessAdapterTest {
    private final PrintActor actor = new PrintActor(1L, 9L, 1L);
    private final PrintIdentity identity = mock(PrintIdentity.class);
    private final BusinessApplicationMapper applications = mock(BusinessApplicationMapper.class);
    private final BusinessApplicationService policy = mock(BusinessApplicationService.class);
    private final BusinessApplicationVersionMapper versions = mock(BusinessApplicationVersionMapper.class);
    private final PrintApplicationLock lock = mock(PrintApplicationLock.class);
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final PrintApplicationAccessAdapter adapter = new PrintApplicationAccessAdapter(
            identity, applications, policy, versions, lock, new PrintApplicationSnapshotCodec(factory.getValidator()));
    @BeforeEach void setup() {
        when(identity.require(anyString())).thenReturn(actor);
        when(identity.current()).thenReturn(actor);
        var application = new AiBusinessApplication();
        application.setId(2L); application.setTenantId(1L); application.setPortalConfig("synthetic-policy");
        when(applications.selectEntityById(1L, 2L)).thenReturn(application);
        when(lock.lock(1L, 2L)).thenReturn(application);
        when(policy.canCurrentUserAccessPortal("synthetic-policy")).thenReturn(true);
    }
    @AfterEach void close() { factory.close(); }

    @Test void requiresBothDesignPermissionAndApplicationScope() {
        adapter.authorize(actor, 2L, PrintDesignAction.VIEW);
        verify(identity).require("print:template:view");
        verify(identity).require("ai:businessApplication:list");
        when(identity.require("ai:businessApplication:publish")).thenThrow(PrintFailure.denied());
        assertThatThrownBy(() -> adapter.authorize(actor, 2L, PrintDesignAction.PUBLISH)).isInstanceOf(BusinessException.class);
        when(policy.canCurrentUserAccessPortal("synthetic-policy")).thenReturn(false);
        assertThatThrownBy(() -> adapter.authorize(actor, 2L, PrintDesignAction.MANAGE)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.lockApplication(actor, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test void rejectsForgedActorTenantAndMissingApplicationBeforeLocking() {
        assertThatThrownBy(() -> adapter.authorize(new PrintActor(2L, 9L, 1L), 2L, PrintDesignAction.VIEW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.lockApplication(new PrintActor(1L, 8L, 1L), 2L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.authorize(actor, 3L, PrintDesignAction.VIEW)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(lock);
    }

    @Test void checksRetainedHistoricalReferencesEvenWhenLatestHasNoneAndFailsClosed() {
        var old = version(snapshot(binding(10, true)));
        when(versions.lockRetainedSnapshots(1L, 2L)).thenReturn(List.of(version("{}"), old));
        assertThatThrownBy(() -> adapter.assertTemplateUnreferenced(actor, 2L, 10L))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getCode()).isEqualTo(409));
        adapter.assertTemplateUnreferenced(actor, 2L, 11L);
        var order = inOrder(lock, versions);
        order.verify(lock).lock(1L, 2L);
        order.verify(versions).lockRetainedSnapshots(1L, 2L);
        when(versions.lockRetainedSnapshots(1L, 2L)).thenReturn(List.of(version("{bad")));
        assertThatThrownBy(() -> adapter.assertTemplateUnreferenced(actor, 2L, 11L)).isInstanceOf(BusinessException.class);
    }

    private AiBusinessApplicationVersion version(String snapshot) {
        var version = new AiBusinessApplicationVersion(); version.setSnapshotJson(snapshot); return version;
    }
}
