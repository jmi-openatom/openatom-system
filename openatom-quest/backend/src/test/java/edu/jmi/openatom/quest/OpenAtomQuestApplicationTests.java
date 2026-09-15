package edu.jmi.openatom.quest;

import static org.assertj.core.api.Assertions.assertThat;

import edu.jmi.openatom.quest.controller.SystemController;
import org.junit.jupiter.api.Test;

class OpenAtomQuestApplicationTests {

    @Test
    void systemControllerCanBeConstructed() {
        assertThat(new SystemController()).isNotNull();
    }
}
