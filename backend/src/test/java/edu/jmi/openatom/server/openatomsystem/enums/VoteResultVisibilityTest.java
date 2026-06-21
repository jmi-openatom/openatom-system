package edu.jmi.openatom.server.openatomsystem.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VoteResultVisibilityTest {

  @Test
  void privateResultsAreNeverVisibleOnSite() {
    assertFalse(VoteResultVisibility.PRIVATE.isVisibleToSite(false, false));
    assertFalse(VoteResultVisibility.PRIVATE.isVisibleToSite(true, false));
    assertFalse(VoteResultVisibility.PRIVATE.isVisibleToSite(false, true));
    assertTrue(VoteResultVisibility.PRIVATE.hidesSiteCounts());
  }

  @Test
  void afterVoteResultsBecomeVisibleAfterVotingOrClosing() {
    assertFalse(VoteResultVisibility.AFTER_VOTE.isVisibleToSite(false, false));
    assertTrue(VoteResultVisibility.AFTER_VOTE.isVisibleToSite(true, false));
    assertTrue(VoteResultVisibility.AFTER_VOTE.isVisibleToSite(false, true));
  }

  @Test
  void legacyBooleanKeepsExistingMeaning() {
    assertEquals(VoteResultVisibility.PUBLIC, VoteResultVisibility.fromLegacy(true));
    assertEquals(VoteResultVisibility.AFTER_VOTE, VoteResultVisibility.fromLegacy(false));
    assertEquals(VoteResultVisibility.PUBLIC, VoteResultVisibility.fromLegacy(null));
  }

  @Test
  void parsesSupportedValuesOnly() {
    assertEquals(VoteResultVisibility.PRIVATE, VoteResultVisibility.fromValue(" private "));
    assertEquals(VoteResultVisibility.AFTER_VOTE, VoteResultVisibility.fromValue("AFTER_VOTE"));
    assertNull(VoteResultVisibility.fromValue("unknown"));
  }
}
