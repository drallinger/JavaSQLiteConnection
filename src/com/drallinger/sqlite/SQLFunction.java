package com.drallinger.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface SQLFunction<T> {
    T execute(ResultSet rs) throws SQLException;
}
