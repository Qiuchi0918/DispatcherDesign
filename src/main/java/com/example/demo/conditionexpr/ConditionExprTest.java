package com.example.demo.conditionexpr;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConditionExprTest {
    public static void main(String[] args) {
        Map<String, Object> param = new HashMap<>();
        List<String> lst = new ArrayList<>();
        lst.add("SomeString");
        param.put("key", lst);
        BooleanConditionExpression expression = new BooleanConditionExpression("key[0]==\"SomeString\"");
        expression.setParameter(param);
        System.out.println(expression.execute());
    }
}
