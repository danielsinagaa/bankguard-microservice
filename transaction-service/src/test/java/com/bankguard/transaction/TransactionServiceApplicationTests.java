package com.bankguard.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TransactionServiceApplicationTests {

	@Test
	void shouldExposeApplicationClass() {
		assertThat(TransactionServiceApplication.class).isNotNull();
	}

}
