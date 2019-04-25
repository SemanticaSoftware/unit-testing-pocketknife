package com.semantica.pocketknife;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertionsEnabledTest {
	
    /**
     * !Very important!
     * Always enable JVM assertions (VM argument '-ea'). If not, the simple test below will fail (and all other tests using the assert keyword).
     */
    @Test
    public void failIfAssertionsNotEnabled() {
        boolean pass = false;
        assert (pass = true);
        Assertions.assertTrue(pass);
    }

}
