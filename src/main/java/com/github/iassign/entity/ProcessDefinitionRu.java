package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 运行时的流程定义表
 * 此表是为了，修改流程定义时，不影响正在运行的实例而存在
 */
@Data
@TableName("t_process_definition_ru")
public class ProcessDefinitionRu {
  @TableId(type = IdType.ASSIGN_ID)
  public String id;
  public String definitionId;
  public Date createTime;
  public String dag;
}
