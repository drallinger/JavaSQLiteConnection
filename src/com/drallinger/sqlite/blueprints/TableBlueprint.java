package com.drallinger.sqlite.blueprints;

public record TableBlueprint(String tableName, boolean ifNotExists, String... columns) {}
