package io.github.yilers.upm.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserExpandHelperTest {

    @Test
    void updatesInitPasswordFlagAndPreservesOtherFields() {
        String initialized = UserExpandHelper.setInitPwd("{\"theme\":\"dark\"}", true);

        assertTrue(UserExpandHelper.isInitPwd(initialized));
        assertTrue(initialized.contains("\"theme\":\"dark\""));

        String changed = UserExpandHelper.setInitPwd(initialized, false);
        assertFalse(UserExpandHelper.isInitPwd(changed));
        assertTrue(changed.contains("\"theme\":\"dark\""));
    }

    @Test
    void treatsMissingOrInvalidFlagAsNotInitialized() {
        assertFalse(UserExpandHelper.isInitPwd(null));
        assertFalse(UserExpandHelper.isInitPwd(""));
        assertFalse(UserExpandHelper.isInitPwd("invalid-json"));
    }
}
