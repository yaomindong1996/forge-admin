package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrintProtocolSmokeTest {

    @Test
    void rejectsMissingProtocolBeforeAnyPersistence() {
        assertThrows(PrintProtocolValidator.InvalidTemplateException.class, () -> new PrintProtocolValidator().validate("{}"));
    }
}
