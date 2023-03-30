package com.drallinger.sqlite.blueprints;

public record PreparedStatementBlueprint(String queryName, String query, boolean returnKeys) {}
