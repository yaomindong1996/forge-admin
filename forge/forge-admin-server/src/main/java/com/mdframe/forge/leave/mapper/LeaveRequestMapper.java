package com.mdframe.forge.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.leave.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假申请 Mapper 接口
 *
 * @author forge
 */
@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

}
