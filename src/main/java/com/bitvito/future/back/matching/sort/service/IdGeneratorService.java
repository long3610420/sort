package com.bitvito.future.back.matching.sort.service;

/**
 * @Description 分布式ID生成接口
 * @author butterfly
 * @date 2017年8月3日 下午2:30:32
 */

public interface IdGeneratorService {

	/**
	 * @return
	 * @Description 生成分布式ID
	 * @author butterfly
	 */
	Long generatorId(String biz);
}
