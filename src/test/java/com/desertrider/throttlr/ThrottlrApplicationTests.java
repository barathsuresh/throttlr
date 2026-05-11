package com.desertrider.throttlr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ThrottlrApplicationTests {

	@Test
	void applicationCanBeConstructed() {
		assertDoesNotThrow(ThrottlrApplication::new);
	}

}
