package io.jenkins.plugins.nexus;

import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce.Wu
 * @date 2025-07-16
 */
public class SimpleTest {

    @Test
    public void testAntMatch() {
        Assertions.assertTrue(SelectorUtils.match("*", "dfadfadf-adsfaf"));
        Assertions.assertTrue(SelectorUtils.match("app-*", "app-131232eraf"));
        Assertions.assertFalse(SelectorUtils.match("app121-*", "app-131232eraf"));
        Assertions.assertTrue(SelectorUtils.match("*-dda*", "app-dda-dadf"));
    }
}
