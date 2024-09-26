package com.bitvito.future.back.matching.sort.idgen.segment.dao;


import com.bitvito.future.back.matching.sort.idgen.segment.model.LeafAlloc;

import java.util.List;

public interface IDAllocDao {
     List<LeafAlloc> getAllLeafAllocs();

     LeafAlloc updateMaxIdAndGetLeafAlloc(String tag);

     LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc);

     List<String> getAllTags();
}
