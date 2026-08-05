package org.likelionhsu.hackathon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class HackathonBeApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void testDatabaseUsesH2() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			String databaseUrl = connection.getMetaData().getURL();

			assertTrue(databaseUrl.startsWith("jdbc:h2:mem:"));
		}
	}
}