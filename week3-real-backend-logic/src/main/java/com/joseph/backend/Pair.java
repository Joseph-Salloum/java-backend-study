package com.joseph.backend;

public class Pair <L, R> {
    private L l;
    private R r;

    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public L getL() {
        return this.l;
    }
    public R getR() {
        return this.r;
    }
}
