package com.bitvito.future.back.matching.sort.idgen.common;


import com.bitvito.future.back.matching.sort.idgen.IDGen;

public class ZeroIDGen implements IDGen {
    @Override
    public Result get(String key) {
        return new Result(0, Status.SUCCESS);
    }

    @Override
    public boolean init() {
        return true;
    }
}
