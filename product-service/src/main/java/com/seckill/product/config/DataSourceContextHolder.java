package com.seckill.product.config;

public class DataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setSlave() { CONTEXT.set("slave"); }
    public static void setMaster() { CONTEXT.set("master"); }
    public static String get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }
}
