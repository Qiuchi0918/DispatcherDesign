package com.example.demo.conditionexpr;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.Map;

public class ConditionExpression {
    public ConditionExpression(String expr) {
        this.expr = expr;
    }

    private Map<String, Object> paramNameValueMap;
    private final String expr;

    /**
     * 设置表达式参数
     *
     * @param paramNameValueMap 表达式参数Map<参数名称,参数值>
     */
    public void setParameter(Map<String, Object> paramNameValueMap) {
        this.paramNameValueMap = paramNameValueMap;
    }

    /**
     * 执行判断
     *
     * @return 判断执行结果
     */
    public boolean execute() {
        Expression compiledExp = AviatorEvaluator.compile(expr);
        Object[] params = new Object[paramNameValueMap.size() * 2];
        int index = -1;
        for (Map.Entry<String, Object> curEntry : paramNameValueMap.entrySet()) {
            params[++index] = curEntry.getKey();
            params[++index] = curEntry.getValue();
        }
        return (Boolean) compiledExp.execute(compiledExp.newEnv(params));
    }
}
