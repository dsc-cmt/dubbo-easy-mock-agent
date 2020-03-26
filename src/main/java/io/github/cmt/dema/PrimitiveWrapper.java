package io.github.cmt.dema;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 用来处理基本类型
 * @author shengchaojie
 * @date 2019-03-04
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrimitiveWrapper implements Serializable {

    private Object data;


}

