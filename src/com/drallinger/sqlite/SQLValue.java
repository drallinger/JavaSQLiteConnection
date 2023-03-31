package com.drallinger.sqlite;

public class SQLValue <T> {
    public enum ValueType{
        INTEGER,
        REAL,
        TEXT
    }
    private final T value;
    private final ValueType type;

    private SQLValue(T value, ValueType type){
        this.value = value;
        this.type = type;
    }

    public static SQLValue<Integer> integer(int value){
        return new SQLValue<>(value, ValueType.INTEGER);
    }

    public static SQLValue<Double> real(double value){
        return new SQLValue<>(value, ValueType.REAL);
    }

    public static SQLValue<String> text(String value){
        return new SQLValue<>(value, ValueType.TEXT);
    }

    public static SQLValue<Integer> bool(boolean value){
        return new SQLValue<>(value ? 1 : 0, ValueType.INTEGER);
    }

    public T getValue(){
        return value;
    }

    public ValueType getType(){
        return type;
    }
}
