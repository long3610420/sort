package com.bitvito.future.back.matching.sort.idgen;


import com.bitvito.future.back.matching.sort.idgen.common.Result;

public interface IDGen {
    Result get(String key);

    boolean init();
}
