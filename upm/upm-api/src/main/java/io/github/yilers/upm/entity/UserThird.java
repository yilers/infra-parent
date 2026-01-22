package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_user_third")
@EqualsAndHashCode(callSuper = true)
public class UserThird extends Model<UserThird> {


}
