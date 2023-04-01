package com.drallinger.sqlite;

public class SQLFunctions {
    public static SQLFunction<Integer> singleInteger(){
        return rs -> rs.getInt(1);
    }

    public static SQLFunction<Double> singleReal(){
        return rs -> rs.getDouble(1);
    }

    public static SQLFunction<String> singleString(){
        return rs -> rs.getString(1);
    }
}
